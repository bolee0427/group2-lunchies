package com.bce.lunchies;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.model.event.AppHomeOpenedEvent;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.slack.api.model.block.Blocks.actions;
import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.model.block.element.BlockElements.button;
import static com.slack.api.model.view.Views.view;

@Configuration
public class SlackConfig {

  @Value("${slack.signing-secret}")
  private String signingSecret;

  @Value("${slack.bot-token}")
  private String botToken;

  @Bean
  public App slackApp() {
    App slackApp = new App(AppConfig.builder()
        .signingSecret(signingSecret)
        .singleTeamBotToken(botToken)
        .build());

    slackApp.blockAction("view_details", (req, ctx) -> {
      String menuIdStr = req.getPayload().getActions().getFirst().getValue();
      Long menuId = Long.parseLong(menuIdStr);

//      Mono<String> menu = menuService.getMenuById(menuId);

      return ctx.ack();
    });

    slackApp.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
      var event = payload.getEvent();

//      List<String> menus = menuService.getMenus().collectList().block();
      List<String> menus = List.of("Menu 1", "Menu 2", "Menu 3");

      var homeView = view(v -> v
          .type("home")
          .blocks(asBlocks(
              header(h -> h.text(plainText("Welcome to Lunchies! 🥗"))),
              section(s -> s.text(markdownText("Pick a menu below to see today's delicious options."))),
              divider(),
              actions(a -> a.elements(
                  menus.stream().map(menu ->
                      button(b -> b.text(plainText(menu))
                          .value(menu.toString())
                          .actionId("view_details"))
                  ).collect(Collectors.toList())
              ))
          ))
      );

      ctx.client().viewsPublish(r -> r
          .userId(event.getUser())
          .view(homeView)
      );

      return ctx.ack();
    });

    return slackApp;
  }
}