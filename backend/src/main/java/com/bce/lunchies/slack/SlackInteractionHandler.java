package com.bce.lunchies.slack;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import com.bce.lunchies.service.AttendanceService;
import com.bce.lunchies.service.MenuService;
import com.bce.lunchies.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.view.View;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SlackInteractionHandler {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final MenuService menuService;
    private final AttendanceService attendanceService;
    private final MethodsClient methodsClient;
    private final HomeTabRenderer homeTabRenderer;
    private final MenuModalBuilder menuModalBuilder;
    private final ChannelMessageBuilder channelMessageBuilder;

    @Value("${slack.channel.id}")
    private String channelId;

    @PostMapping(value = "/slack/interactions", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> handleInteraction(ServerWebExchange exchange) {
        return exchange.getFormData()
                .map(formData -> formData.getFirst("payload"))
                .flatMap(payload -> {
                    log.info("Interaction received: {}", payload != null ? payload.substring(0, Math.min(200, payload.length())) : "null");
                    return Mono.fromCallable(() -> objectMapper.readTree(payload));
                })
                .flatMap(json -> {
                    String type = json.path("type").asText();
                    log.info("Interaction type: {}", type);

                    if ("block_actions".equals(type)) {
                        return handleBlockAction(json);
                    }

                    if ("view_submission".equals(type)) {
                        return handleViewSubmission(json).thenReturn("");
                    }

                    return Mono.just("");
                })
                .onErrorResume(e -> {
                    log.error("Interaction handling failed", e);
                    return Mono.just("");
                });
    }

    private Mono<String> handleBlockAction(JsonNode json) {
        String slackUserId = json.path("user").path("id").asText();
        String triggerId = json.path("trigger_id").asText();
        JsonNode action = json.path("actions").get(0);
        String actionId = action.path("action_id").asText();

        log.info("Action ID: {}", actionId);

        // Create menu opens a modal — needs trigger_id, no home tab refresh
        if ("create_menu_btn".equals(actionId)) {
            return openCreateMenuModal(triggerId).thenReturn("");
        }

        // Preview as different role (SUPER_ADMIN only)
        if (actionId.startsWith("preview_as_")) {
            String roleName = actionId.substring("preview_as_".length()).toUpperCase();
            Role previewRole = Role.valueOf(roleName);
            return renderAsRole(slackUserId, previewRole).thenReturn("");
        }

        // Add one more item slot to the create menu modal
        if ("add_menu_item".equals(actionId)) {
            String viewId = json.path("view").path("id").asText();
            int currentCount = Integer.parseInt(action.path("value").asText());
            return updateModalWithMoreItems(viewId, currentCount + 1).thenReturn("");
        }

        return userService.findOrCreate(slackUserId)
                .flatMap(user -> routeAction(actionId, user))
                .then(refreshHomeTab(slackUserId))
                .thenReturn("")
                .doOnError(e -> log.error("Block action failed for {}", actionId, e));
    }

    private Mono<Void> routeAction(String actionId, AppUser user) {
        // RSVP buttons: rsvp_yes_2026-03-17 or rsvp_no_2026-03-17
        if (actionId.startsWith("rsvp_yes_")) {
            LocalDate date = LocalDate.parse(actionId.substring("rsvp_yes_".length()));
            return attendanceService.rsvp(user.getId(), date, true);
        }
        if (actionId.startsWith("rsvp_no_")) {
            LocalDate date = LocalDate.parse(actionId.substring("rsvp_no_".length()));
            return attendanceService.rsvp(user.getId(), date, false);
        }

        // Admin: publish menu
        if (actionId.startsWith("publish_menu_")) {
            UUID menuId = UUID.fromString(actionId.substring("publish_menu_".length()));
            return publishMenu(menuId);
        }

        // Admin: unpublish menu
        if (actionId.startsWith("unpublish_menu_")) {
            UUID menuId = UUID.fromString(actionId.substring("unpublish_menu_".length()));
            return unpublishMenu(menuId);
        }

        // Admin: delete menu
        if (actionId.startsWith("delete_menu_")) {
            UUID menuId = UUID.fromString(actionId.substring("delete_menu_".length()));
            return menuService.deleteMenu(menuId);
        }

        // Super admin: promote/demote
        if (actionId.startsWith("promote_user_")) {
            UUID userId = UUID.fromString(actionId.substring("promote_user_".length()));
            return userService.promoteToAdmin(userId);
        }
        if (actionId.startsWith("demote_user_")) {
            UUID userId = UUID.fromString(actionId.substring("demote_user_".length()));
            return userService.demoteToUser(userId);
        }

        log.warn("Unknown action_id: {}", actionId);
        return Mono.empty();
    }

    private Mono<Void> publishMenu(UUID menuId) {
        log.info("Publishing menu {}, channel: {}", menuId, channelId);
        return menuService.findById(menuId)
                .doOnNext(menu -> log.info("Found menu: {}", menu.getMenuDate()))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Menu not found: {}", menuId);
                    return Mono.empty();
                }))
                .flatMap(menu -> menuService.getMenuItems(menuId).collectList()
                        .flatMap(items -> Mono.<String>fromCallable(() -> {
                            var blocks = channelMessageBuilder.buildMenuMessage(menu, items);
                            var fallback = channelMessageBuilder.buildFallbackText(menu, items);
                            var result = methodsClient.chatPostMessage(r -> r
                                    .channel(channelId)
                                    .blocks(blocks)
                                    .text(fallback)
                            );
                            if (!result.isOk()) {
                                log.error("Failed to post menu: {}", result.getError());
                            }
                            return result.getTs();
                        }).subscribeOn(Schedulers.boundedElastic())))
                .flatMap(ts -> menuService.updateSlackMessageTs(menuId, ts));
    }

    private Mono<Void> unpublishMenu(UUID menuId) {
        return menuService.findById(menuId)
                .flatMap(menu -> {
                    if (menu.getSlackMessageTs() == null) return Mono.empty();
                    return Mono.<Void>fromCallable(() -> {
                        methodsClient.chatDelete(r -> r
                                .channel(channelId)
                                .ts(menu.getSlackMessageTs()));
                        return null;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .then(menuService.updateSlackMessageTs(menuId, null));
    }

    private Mono<Void> openCreateMenuModal(String triggerId) {
        View modal = menuModalBuilder.buildCreateModal();
        return Mono.<Void>fromCallable(() -> {
            methodsClient.viewsOpen(r -> r
                    .triggerId(triggerId)
                    .view(modal));
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> updateModalWithMoreItems(String viewId, int newCount) {
        View updatedModal = menuModalBuilder.buildModalWithCount(newCount);
        return Mono.<Void>fromCallable(() -> {
            methodsClient.viewsUpdate(r -> r
                    .viewId(viewId)
                    .view(updatedModal));
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> handleViewSubmission(JsonNode json) {
        String callbackId = json.path("view").path("callback_id").asText();
        String slackUserId = json.path("user").path("id").asText();

        if ("create_menu".equals(callbackId)) {
            return handleCreateMenuSubmission(json, slackUserId);
        }

        return Mono.empty();
    }

    private Mono<Void> handleCreateMenuSubmission(JsonNode json, String slackUserId) {
        JsonNode values = json.path("view").path("state").path("values");

        // Parse date
        String dateStr = values.path("menu_date_block").path("menu_date").path("selected_date").asText();
        LocalDate menuDate = LocalDate.parse(dateStr);

        // Parse title
        String title = values.path("menu_title_block").path("menu_title").path("value").asText(null);

        List<MenuService.MenuItemInput> items = parseItemsFromValues(values);

        return userService.findOrCreate(slackUserId)
                .flatMap(user -> menuService.createMenu(menuDate, title, user.getId()))
                .flatMap(menu -> {
                    if (items.isEmpty()) return Mono.<Void>empty();
                    return menuService.replaceMenuItems(menu.getId(), items);
                })
                .then(refreshHomeTab(slackUserId));
    }

    private List<MenuService.MenuItemInput> parseItemsFromValues(JsonNode values) {
        List<MenuService.MenuItemInput> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String name = values.path("item_name_" + i).path("item_name").path("value").asText(null);
            if (name == null || name.isBlank()) continue;

            String desc = values.path("item_desc_" + i).path("item_desc").path("value").asText(null);
            String[] tags = parseMultiSelect(values.path("item_tags_" + i).path("item_tags"));
            String[] allergens = parseMultiSelect(values.path("item_allergens_" + i).path("item_allergens"));

            items.add(new MenuService.MenuItemInput(name, desc, tags, allergens));
        }
        return items;
    }

    private String[] parseMultiSelect(JsonNode node) {
        JsonNode selected = node.path("selected_options");
        if (selected.isMissingNode() || !selected.isArray() || selected.isEmpty()) {
            return new String[0];
        }
        String[] result = new String[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            result[i] = selected.get(i).path("value").asText();
        }
        return result;
    }

    private Mono<Void> renderAsRole(String slackUserId, Role role) {
        return userService.findOrCreate(slackUserId)
                .flatMap(user -> homeTabRenderer.render(user, role))
                .flatMap(homeTab -> Mono.<Void>fromCallable(() -> {
                    methodsClient.viewsPublish(r -> r
                            .userId(slackUserId)
                            .view(homeTab));
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Void> refreshHomeTab(String slackUserId) {
        return userService.findOrCreate(slackUserId)
                .flatMap(user -> homeTabRenderer.render(user))
                .flatMap(homeTab -> Mono.<Void>fromCallable(() -> {
                    methodsClient.viewsPublish(r -> r
                            .userId(slackUserId)
                            .view(homeTab));
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
