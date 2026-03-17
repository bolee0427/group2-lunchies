package com.bce.lunchies.slack;

import com.bce.lunchies.model.*;
import com.bce.lunchies.service.AttendanceService;
import com.bce.lunchies.service.MenuService;
import com.bce.lunchies.service.UserService;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.Views;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

@Component
@RequiredArgsConstructor
public class HomeTabRenderer {

    private final MenuService menuService;
    private final AttendanceService attendanceService;
    private final UserService userService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE MMM d");

    private static final Map<String, String> TAG_EMOJI = Map.of(
            "VEGAN", ":seedling:",
            "VEGETARIAN", ":herb:",
            "PESCATARIAN", ":fish:",
            "GLUTEN_FREE", ":ear_of_rice:",
            "DAIRY_FREE", ":glass_of_milk:",
            "HALAL", ":crescent_moon:",
            "KOSHER", ":star_of_david:"
    );

    public Mono<View> render(AppUser user) {
        return render(user, user.getRole());
    }

    public Mono<View> render(AppUser user, Role asRole) {
        return switch (asRole) {
            case SUPER_ADMIN -> renderSuperAdminView(user);
            case ADMIN -> renderAdminView(user);
            case USER -> renderUserView(user);
        };
    }

    // ── USER VIEW ──

    private Mono<View> renderUserView(AppUser user) {
        LocalDate today = LocalDate.now();

        Mono<List<LayoutBlock>> menuBlocks = renderTodayMenu(today);
        Mono<List<LayoutBlock>> rsvpBlocks = renderRsvpSection(user);
        Mono<List<LayoutBlock>> headcountBlocks = renderHeadcount(today);

        return Mono.zip(menuBlocks, rsvpBlocks, headcountBlocks)
                .map(tuple -> {
                    List<LayoutBlock> blocks = new ArrayList<>();
                    blocks.add(header(h -> h.text(plainText("Today's Lunch — " + today.format(DATE_FMT)))));
                    blocks.add(divider());
                    blocks.addAll(tuple.getT1());
                    blocks.add(divider());
                    blocks.addAll(tuple.getT3());
                    blocks.add(divider());
                    blocks.add(header(h -> h.text(plainText("RSVP — Upcoming Week"))));
                    blocks.addAll(tuple.getT2());
                    return toView(blocks);
                });
    }

    private Mono<List<LayoutBlock>> renderTodayMenu(LocalDate today) {
        return menuService.findByDate(today)
                .flatMap(menu -> menuService.getMenuItems(menu.getId()).collectList()
                        .map(items -> {
                            List<LayoutBlock> blocks = new ArrayList<>();
                            if (menu.getTitle() != null) {
                                blocks.add(section(s -> s.text(markdownText("*" + menu.getTitle() + "*"))));
                            }
                            if (items.isEmpty()) {
                                blocks.add(section(s -> s.text(markdownText("_No items listed._"))));
                            } else {
                                for (MenuItem item : items) {
                                    blocks.add(section(s -> s.text(markdownText(formatMenuItem(item)))));
                                }
                            }
                            return blocks;
                        }))
                .defaultIfEmpty(List.of(
                        section(s -> s.text(markdownText("No menu posted yet for today.")))
                ));
    }

    private Mono<List<LayoutBlock>> renderHeadcount(LocalDate today) {
        return attendanceService.getHeadcount(today)
                .map(count -> List.<LayoutBlock>of(
                        section(s -> s.text(markdownText(":busts_in_silhouette: *" + count + " people* attending today")))
                ))
                .defaultIfEmpty(List.of(
                        section(s -> s.text(markdownText(":busts_in_silhouette: No attendance data yet")))
                ));
    }

    private Mono<List<LayoutBlock>> renderRsvpSection(AppUser user) {
        return attendanceService.getUpcomingAttendance(user.getId()).collectList()
                .map(existing -> {
                    Map<LocalDate, Boolean> rsvpMap = new HashMap<>();
                    existing.forEach(a -> rsvpMap.put(a.getAttendanceDate(), a.isAttending()));

                    List<LayoutBlock> blocks = new ArrayList<>();
                    List<LocalDate> weekdays = getUpcomingWeekdays();

                    for (LocalDate date : weekdays) {
                        Boolean current = rsvpMap.get(date);
                        String dateStr = date.format(DATE_FMT);
                        String status = current == null ? "" : (current ? " :white_check_mark:" : " :x:");

                        String yesStyle = current != null && current ? "primary" : null;
                        String noStyle = current != null && !current ? "danger" : null;

                        blocks.add(section(s -> s
                                .text(markdownText("*" + dateStr + "*" + status))
                                .accessory(null)));
                        blocks.add(actions(a -> a.elements(List.<BlockElement>of(
                                button(b -> {
                                    b.text(plainText("Yes")).actionId("rsvp_yes_" + date);
                                    if (yesStyle != null) b.style(yesStyle);
                                    return b;
                                }),
                                button(b -> {
                                    b.text(plainText("No")).actionId("rsvp_no_" + date);
                                    if (noStyle != null) b.style(noStyle);
                                    return b;
                                })
                        ))));
                    }
                    return blocks;
                });
    }

    // ── ADMIN VIEW ──

    private Mono<View> renderAdminView(AppUser user) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate weekEnd = tomorrow.plusDays(7);

        Mono<List<LayoutBlock>> menuListBlocks = renderUpcomingMenus(tomorrow, weekEnd);
        Mono<List<LayoutBlock>> attendanceSummary = renderAttendanceSummary(tomorrow, weekEnd);

        return Mono.zip(menuListBlocks, attendanceSummary)
                .map(tuple -> {
                    List<LayoutBlock> blocks = new ArrayList<>();
                    blocks.add(header(h -> h.text(plainText("LunchBoard — Kitchen Dashboard"))));
                    blocks.add(actions(a -> a.elements(List.<BlockElement>of(
                            button(b -> b
                                    .text(plainText("Create Menu"))
                                    .actionId("create_menu_btn")
                                    .style("primary"))
                    ))));
                    blocks.add(divider());
                    blocks.add(header(h -> h.text(plainText("Upcoming Menus"))));
                    blocks.addAll(tuple.getT1());
                    blocks.add(divider());
                    blocks.add(header(h -> h.text(plainText("Attendance Summary"))));
                    blocks.addAll(tuple.getT2());
                    return toView(blocks);
                });
    }

    private Mono<List<LayoutBlock>> renderUpcomingMenus(LocalDate from, LocalDate to) {
        return menuService.findByDateRange(from, to).collectList()
                .map(menus -> {
                    List<LayoutBlock> blocks = new ArrayList<>();
                    if (menus.isEmpty()) {
                        blocks.add(section(s -> s.text(markdownText("_No upcoming menus._"))));
                        return blocks;
                    }
                    for (Menu menu : menus) {
                        String dateStr = menu.getMenuDate().format(DATE_FMT);
                        String title = menu.getTitle() != null ? menu.getTitle() : "Untitled";
                        boolean published = menu.getSlackMessageTs() != null;
                        String badge = published ? ":large_green_circle: Published" : ":white_circle: Draft";
                        String menuId = menu.getId().toString();

                        blocks.add(section(s -> s.text(markdownText(
                                "*" + dateStr + "* — " + title + "\n" + badge))));

                        List<BlockElement> buttons = new ArrayList<>();
                        if (published) {
                            buttons.add(button(b -> b.text(plainText("Unpublish")).actionId("unpublish_menu_" + menuId)));
                        } else {
                            buttons.add(button(b -> b.text(plainText("Publish")).actionId("publish_menu_" + menuId).style("primary")));
                            buttons.add(button(b -> b.text(plainText("Delete")).actionId("delete_menu_" + menuId).style("danger")));
                        }
                        blocks.add(actions(a -> a.elements(buttons)));
                    }
                    return blocks;
                });
    }

    private Mono<List<LayoutBlock>> renderAttendanceSummary(LocalDate from, LocalDate to) {
        List<LocalDate> weekdays = getWeekdaysBetween(from, to);
        if (weekdays.isEmpty()) {
            return Mono.just(List.of(section(s -> s.text(markdownText("_No upcoming weekdays._")))));
        }

        List<Mono<String>> lines = weekdays.stream()
                .map(date -> attendanceService.getHeadcount(date)
                        .defaultIfEmpty(0)
                        .map(count -> date.format(DATE_FMT) + ": *" + count + "* attending"))
                .toList();

        return Mono.zip(lines, results -> {
            StringBuilder sb = new StringBuilder();
            for (Object r : results) {
                sb.append(r).append("\n");
            }
            return List.<LayoutBlock>of(section(s -> s.text(markdownText(sb.toString().trim()))));
        });
    }

    // ── SUPER_ADMIN VIEW ──

    private Mono<View> renderSuperAdminView(AppUser user) {
        return renderAdminView(user)
                .flatMap(adminView -> userService.findAll().collectList()
                        .map(users -> {
                            List<LayoutBlock> blocks = new ArrayList<>(adminView.getBlocks());
                            blocks.add(divider());
                            blocks.add(header(h -> h.text(plainText("User Management"))));

                            for (AppUser u : users) {
                                String name = u.getDisplayName() != null ? u.getDisplayName() : u.getSlackUserId();
                                String role = u.getRole().name();
                                String userId = u.getId().toString();

                                blocks.add(section(s -> s.text(markdownText(
                                        "*" + name + "* — " + role))));

                                if (u.getRole() == Role.USER) {
                                    blocks.add(actions(a -> a.elements(List.<BlockElement>of(
                                            button(b -> b.text(plainText("Promote to Admin"))
                                                    .actionId("promote_user_" + userId)
                                                    .style("primary"))
                                    ))));
                                } else if (u.getRole() == Role.ADMIN) {
                                    blocks.add(actions(a -> a.elements(List.<BlockElement>of(
                                            button(b -> b.text(plainText("Demote to User"))
                                                    .actionId("demote_user_" + userId)
                                                    .style("danger"))
                                    ))));
                                }
                            }

                            blocks.add(divider());
                            blocks.add(header(h -> h.text(plainText("Preview"))));
                            blocks.add(actions(a -> a.elements(List.<BlockElement>of(
                                    button(b -> b.text(plainText("View as User"))
                                            .actionId("preview_as_user")),
                                    button(b -> b.text(plainText("View as Admin"))
                                            .actionId("preview_as_admin")),
                                    button(b -> b.text(plainText("Back to Super Admin"))
                                            .actionId("preview_as_super_admin"))
                            ))));

                            return toView(blocks);
                        }));
    }

    // ── HELPERS ──

    private String formatMenuItem(MenuItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(item.getName()).append("*");
        if (item.getDescription() != null) {
            sb.append("\n").append(item.getDescription());
        }
        if (item.getTags() != null) {
            for (String tag : item.getTags()) {
                String emoji = TAG_EMOJI.getOrDefault(tag, "");
                sb.append(" ").append(emoji);
            }
        }
        if (item.getAllergens() != null && item.getAllergens().length > 0) {
            sb.append("\n:warning: Allergens: ");
            sb.append(String.join(", ", item.getAllergens()));
        }
        return sb.toString();
    }

    private List<LocalDate> getUpcomingWeekdays() {
        return getWeekdaysBetween(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7));
    }

    private List<LocalDate> getWeekdaysBetween(LocalDate from, LocalDate to) {
        return Stream.iterate(from, d -> d.plusDays(1))
                .limit(java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1)
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .toList();
    }

    private View toView(List<LayoutBlock> blocks) {
        return Views.view(v -> v.type("home").blocks(blocks));
    }
}
