package com.bce.lunchies.slack;

import com.bce.lunchies.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.view.View;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SlackEventHandler {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final MethodsClient methodsClient;
    private final HomeTabRenderer homeTabRenderer;

    @PostMapping("/slack/events")
    public Mono<Map<String, Object>> handleEvent(@RequestBody String body) {
        return Mono.fromCallable(() -> objectMapper.readTree(body))
                .flatMap(json -> {
                    String type = json.path("type").asText();

                    if ("url_verification".equals(type)) {
                        String challenge = json.path("challenge").asText();
                        return Mono.just(Map.<String, Object>of("challenge", challenge));
                    }

                    if ("event_callback".equals(type)) {
                        JsonNode event = json.path("event");
                        String eventType = event.path("type").asText();

                        if ("app_home_opened".equals(eventType)) {
                            String slackUserId = event.path("user").asText();
                            return handleAppHomeOpened(slackUserId)
                                    .thenReturn(Map.<String, Object>of("ok", true));
                        }
                    }

                    return Mono.just(Map.<String, Object>of("ok", true));
                });
    }

    private Mono<Void> handleAppHomeOpened(String slackUserId) {
        return userService.findOrCreate(slackUserId)
                .flatMap(user -> homeTabRenderer.render(user))
                .flatMap(homeTab -> Mono.<Void>fromCallable(() -> {
                    methodsClient.viewsPublish(r -> r
                            .userId(slackUserId)
                            .view(homeTab));
                    return null;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(e -> log.error("Failed to render Home tab for {}", slackUserId, e));
    }
}
