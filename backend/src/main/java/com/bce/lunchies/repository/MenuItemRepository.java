package com.bce.lunchies.repository;

import com.bce.lunchies.model.MenuItem;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.bce.lunchies.repository.Tables.MenuItem.*;

@Repository
@RequiredArgsConstructor
public class MenuItemRepository {

    private final DSLContext dsl;

    public Flux<MenuItem> findByMenuId(UUID menuId) {
        return Flux.from(dsl
                .selectFrom(TABLE)
                .where(MENU_ID.eq(menuId))
                .orderBy(SORT_ORDER)
        ).map(this::toMenuItem);
    }

    public Mono<MenuItem> create(UUID menuId, String name, String description, int sortOrder, String[] tags, String[] allergens) {
        return Mono.from(dsl
                .insertInto(TABLE)
                .columns(MENU_ID, NAME, DESCRIPTION, SORT_ORDER, TAGS, ALLERGENS)
                .values(menuId, name, description, sortOrder, tags, allergens)
                .returningResult(ID, MENU_ID, NAME, DESCRIPTION, SORT_ORDER, TAGS, ALLERGENS)
        ).map(this::toMenuItem);
    }

    public Mono<Void> deleteByMenuId(UUID menuId) {
        return Mono.from(dsl
                .deleteFrom(TABLE)
                .where(MENU_ID.eq(menuId))
        ).then();
    }

    private MenuItem toMenuItem(org.jooq.Record record) {
        MenuItem item = new MenuItem();
        item.setId(record.get(ID));
        item.setMenuId(record.get(MENU_ID));
        item.setName(record.get(NAME));
        item.setDescription(record.get(DESCRIPTION));
        item.setSortOrder(record.get(SORT_ORDER));
        item.setTags(record.get(TAGS));
        item.setAllergens(record.get(ALLERGENS));
        return item;
    }
}
