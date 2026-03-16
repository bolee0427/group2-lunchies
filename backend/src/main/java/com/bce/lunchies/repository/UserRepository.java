package com.bce.lunchies.repository;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.bce.lunchies.repository.Tables.AppUser.*;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;

    public Mono<AppUser> findBySlackUserId(String slackUserId) {
        return Mono.from(dsl
                .selectFrom(TABLE)
                .where(SLACK_USER_ID.eq(slackUserId))
        ).map(this::toAppUser);
    }

    public Mono<AppUser> findById(UUID id) {
        return Mono.from(dsl
                .selectFrom(TABLE)
                .where(ID.eq(id))
        ).map(this::toAppUser);
    }

    public Flux<AppUser> findAll() {
        return Flux.from(dsl
                .selectFrom(TABLE)
                .orderBy(DISPLAY_NAME)
        ).map(this::toAppUser);
    }

    public Mono<AppUser> create(String slackUserId, String displayName, String email, Role role) {
        return Mono.from(dsl
                .insertInto(TABLE)
                .columns(SLACK_USER_ID, DISPLAY_NAME, EMAIL, ROLE, CREATED_AT)
                .values(slackUserId, displayName, email, role.name(), OffsetDateTime.now())
                .returningResult(ID, SLACK_USER_ID, EMAIL, DISPLAY_NAME, ROLE, CREATED_AT, LAST_LOGIN)
        ).map(this::toAppUser);
    }

    public Mono<Void> updateRole(UUID id, Role role) {
        return Mono.from(dsl
                .update(TABLE)
                .set(ROLE, role.name())
                .where(ID.eq(id))
        ).then();
    }

    public Mono<Void> updateLastLogin(UUID id) {
        return Mono.from(dsl
                .update(TABLE)
                .set(LAST_LOGIN, OffsetDateTime.now())
                .where(ID.eq(id))
        ).then();
    }

    private AppUser toAppUser(org.jooq.Record record) {
        AppUser user = new AppUser();
        user.setId(record.get(ID));
        user.setSlackUserId(record.get(SLACK_USER_ID));
        user.setEmail(record.get(EMAIL));
        user.setDisplayName(record.get(DISPLAY_NAME));
        user.setRole(Role.valueOf(record.get(ROLE)));
        user.setCreatedAt(record.get(CREATED_AT));
        user.setLastLogin(record.get(LAST_LOGIN));
        return user;
    }
}
