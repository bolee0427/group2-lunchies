package com.bce.lunchies.slack;

import com.bce.lunchies.model.Allergen;
import com.bce.lunchies.model.DietaryTag;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.MultiStaticSelectElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;

@Component
public class MenuModalBuilder {

    private static final int DEFAULT_ITEMS = 3;
    private static final int MAX_ITEMS = 10;

    public View buildCreateModal() {
        return buildModal(DEFAULT_ITEMS);
    }

    public View buildModalWithCount(int itemCount) {
        return buildModal(Math.min(itemCount, MAX_ITEMS));
    }

    private View buildModal(int itemCount) {
        List<LayoutBlock> blocks = new ArrayList<>();

        // Date picker
        blocks.add(input(i -> i
                .blockId("menu_date_block")
                .label(plainText("Date"))
                .element(datePicker(d -> d.actionId("menu_date")))));

        // Title
        blocks.add(input(i -> i
                .blockId("menu_title_block")
                .optional(true)
                .label(plainText("Menu Title"))
                .element(plainTextInput(t -> t.actionId("menu_title").placeholder(plainText("e.g. Tuesday Special"))))));

        // Menu items
        for (int n = 0; n < itemCount; n++) {
            final int idx = n;
            boolean required = idx == 0;

            blocks.add(header(h -> h.text(plainText("Item " + (idx + 1)))));

            blocks.add(input(i -> i
                    .blockId("item_name_" + idx)
                    .optional(!required)
                    .label(plainText("Name"))
                    .element(plainTextInput(t -> t.actionId("item_name").placeholder(plainText("e.g. Tomato Soup"))))));

            blocks.add(input(i -> i
                    .blockId("item_desc_" + idx)
                    .optional(true)
                    .label(plainText("Description"))
                    .element(plainTextInput(t -> t.actionId("item_desc").placeholder(plainText("Short description"))))));

            blocks.add(input(i -> i
                    .blockId("item_tags_" + idx)
                    .optional(true)
                    .label(plainText("Dietary Tags"))
                    .element(MultiStaticSelectElement.builder()
                            .actionId("item_tags")
                            .options(tagOptions())
                            .build())));

            blocks.add(input(i -> i
                    .blockId("item_allergens_" + idx)
                    .optional(true)
                    .label(plainText("Allergens"))
                    .element(MultiStaticSelectElement.builder()
                            .actionId("item_allergens")
                            .options(allergenOptions())
                            .build())));
        }

        // "Add Item" button (if not at max)
        if (itemCount < MAX_ITEMS) {
            blocks.add(actions(a -> a
                    .blockId("add_item_action")
                    .elements(List.<BlockElement>of(
                            button(b -> b
                                    .text(plainText("+ Add Menu Item"))
                                    .actionId("add_menu_item")
                                    .value(String.valueOf(itemCount)))))));
        }

        return View.builder()
                .type("modal")
                .callbackId("create_menu")
                .privateMetadata(String.valueOf(itemCount))
                .title(ViewTitle.builder().type("plain_text").text("Create Menu").build())
                .submit(ViewSubmit.builder().type("plain_text").text("Save").build())
                .close(ViewClose.builder().type("plain_text").text("Cancel").build())
                .blocks(blocks)
                .build();
    }

    private List<OptionObject> tagOptions() {
        return Arrays.stream(DietaryTag.values())
                .map(tag -> OptionObject.builder()
                        .text(plainText(formatLabel(tag.name())))
                        .value(tag.name())
                        .build())
                .toList();
    }

    private List<OptionObject> allergenOptions() {
        return Arrays.stream(Allergen.values())
                .map(a -> OptionObject.builder()
                        .text(plainText(formatLabel(a.name())))
                        .value(a.name())
                        .build())
                .toList();
    }

    private String formatLabel(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase().replace('_', ' ');
    }
}
