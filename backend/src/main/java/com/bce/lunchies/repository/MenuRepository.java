package com.bce.lunchies.repository;

import com.bce.lunchies.model.Menu;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.bce.lunchies.repository.Tables.Menu.*;

@Repository
@RequiredArgsConstructor
public class MenuRepository {

    private final DSLContext dsl;

    public Mono<Menu> findById(UUID id) {
        return Mono.from(dsl
                .selectFrom(TABLE)
                .where(ID.eq(id))
        ).map(this::toMenu);
    }

    public Mono<Menu> findByDate(LocalDate date) {
        return Mono.from(dsl
                .selectFrom(TABLE)
                .where(MENU_DATE.eq(date))
        ).map(this::toMenu);
    }

    public Flux<Menu> findByDateRange(LocalDate from, LocalDate to) {
        return Flux.from(dsl
                .selectFrom(TABLE)
                .where(MENU_DATE.between(from, to))
                .orderBy(MENU_DATE)
        ).map(this::toMenu);
    }

    public Mono<Menu> create(LocalDate menuDate, String title, UUID createdBy) {
        OffsetDateTime now = OffsetDateTime.now();
        return Mono.from(dsl
                .insertInto(TABLE)
                .columns(MENU_DATE, TITLE, CREATED_BY, CREATED_AT, UPDATED_AT)
                .values(menuDate, title, createdBy, now, now)
                .returningResult(ID, MENU_DATE, TITLE, CREATED_BY, SLACK_MESSAGE_TS, CREATED_AT, UPDATED_AT)
        ).map(this::toMenu);
    }

    public Mono<Void> updateSlackMessageTs(UUID id, String slackMessageTs) {
        return Mono.from(dsl
                .update(TABLE)
                .set(SLACK_MESSAGE_TS, slackMessageTs)
                .set(UPDATED_AT, OffsetDateTime.now())
                .where(ID.eq(id))
        ).then();
    }

    public Mono<Void> delete(UUID id) {
        return Mono.from(dsl
                .deleteFrom(TABLE)
                .where(ID.eq(id))
        ).then();
    }

    private Menu toMenu(org.jooq.Record record) {
        Menu menu = new Menu();
        menu.setId(record.get(ID));
        menu.setMenuDate(Tables.getLocalDate(record, MENU_DATE));
        menu.setTitle(record.get(TITLE));
        menu.setCreatedBy(record.get(CREATED_BY));
        menu.setSlackMessageTs(record.get(SLACK_MESSAGE_TS));
        menu.setCreatedAt(record.get(CREATED_AT));
        menu.setUpdatedAt(record.get(UPDATED_AT));
        return menu;
    }
}
