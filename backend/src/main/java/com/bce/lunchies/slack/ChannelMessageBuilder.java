package com.bce.lunchies.slack;

import com.bce.lunchies.model.Menu;
import com.bce.lunchies.model.MenuItem;
import com.slack.api.model.block.LayoutBlock;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;

@Component
public class ChannelMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMM d");

    private static final Map<String, String> TAG_EMOJI = Map.of(
            "VEGAN", ":seedling: Vegan",
            "VEGETARIAN", ":herb: Vegetarian",
            "PESCATARIAN", ":fish: Pescatarian",
            "GLUTEN_FREE", ":ear_of_rice: Gluten-Free",
            "DAIRY_FREE", ":glass_of_milk: Dairy-Free",
            "HALAL", ":crescent_moon: Halal",
            "KOSHER", ":star_of_david: Kosher"
    );

    public List<LayoutBlock> buildMenuMessage(Menu menu, List<MenuItem> items) {
        List<LayoutBlock> blocks = new ArrayList<>();

        // Header
        String title = menu.getTitle() != null ? menu.getTitle() : "Lunch Menu";
        String dateStr = menu.getMenuDate().format(DATE_FMT);
        blocks.add(header(h -> h.text(plainText(title + " — " + dateStr))));
        blocks.add(divider());

        // Menu items
        for (MenuItem item : items) {
            blocks.add(section(s -> s.text(markdownText(formatItem(item)))));
        }

        if (items.isEmpty()) {
            blocks.add(section(s -> s.text(markdownText("_No items listed._"))));
        }

        blocks.add(divider());
        blocks.add(context(c -> c.elements(List.of(
                markdownText(":fork_and_knife: Posted by LunchBoard")
        ))));

        return blocks;
    }

    public String buildFallbackText(Menu menu, List<MenuItem> items) {
        StringBuilder sb = new StringBuilder();
        String title = menu.getTitle() != null ? menu.getTitle() : "Lunch Menu";
        sb.append(title).append(" — ").append(menu.getMenuDate().format(DATE_FMT)).append("\n");
        for (MenuItem item : items) {
            sb.append("• ").append(item.getName());
            if (item.getDescription() != null) sb.append(" — ").append(item.getDescription());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatItem(MenuItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(item.getName()).append("*");

        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            sb.append("\n").append(item.getDescription());
        }

        if (item.getTags() != null && item.getTags().length > 0) {
            sb.append("\n");
            for (String tag : item.getTags()) {
                String label = TAG_EMOJI.getOrDefault(tag, tag);
                sb.append(label).append("  ");
            }
        }

        if (item.getAllergens() != null && item.getAllergens().length > 0) {
            sb.append("\n:warning: Allergens: ");
            sb.append(String.join(", ", item.getAllergens()));
        }

        return sb.toString();
    }
}
