package com.bce.lunchies.repository;

import com.bce.lunchies.model.Attendance;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.bce.lunchies.repository.Tables.Attendance.*;
import static org.jooq.impl.DSL.count;

@Repository
@RequiredArgsConstructor
public class AttendanceRepository {

    private final DSLContext dsl;

    public Mono<Void> upsert(LocalDate date, UUID userId, boolean attending) {
        OffsetDateTime now = OffsetDateTime.now();
        return Mono.from(dsl
                .insertInto(TABLE)
                .columns(ATTENDANCE_DATE, USER_ID, ATTENDING, UPDATED_AT)
                .values(date, userId, attending, now)
                .onConflict(ATTENDANCE_DATE, USER_ID)
                .doUpdate()
                .set(ATTENDING, attending)
                .set(UPDATED_AT, now)
        ).then();
    }

    public Mono<Attendance> findByDateAndUserId(LocalDate date, UUID userId) {
        return Mono.from(dsl
                .selectFrom(TABLE)
                .where(ATTENDANCE_DATE.eq(date).and(USER_ID.eq(userId)))
        ).map(this::toAttendance);
    }

    public Flux<Attendance> findByUserIdAndDateRange(UUID userId, LocalDate from, LocalDate to) {
        return Flux.from(dsl
                .selectFrom(TABLE)
                .where(USER_ID.eq(userId).and(ATTENDANCE_DATE.between(from, to)))
                .orderBy(ATTENDANCE_DATE)
        ).map(this::toAttendance);
    }

    public Mono<Integer> countAttendingByDate(LocalDate date) {
        return Mono.from(dsl
                .select(count().as("cnt"))
                .from(TABLE)
                .where(ATTENDANCE_DATE.eq(date).and(ATTENDING.eq(true)))
        ).map(record -> record.get("cnt", Integer.class));
    }

    public Flux<Attendance> findByDate(LocalDate date) {
        return Flux.from(dsl
                .selectFrom(TABLE)
                .where(ATTENDANCE_DATE.eq(date))
        ).map(this::toAttendance);
    }

    private Attendance toAttendance(org.jooq.Record record) {
        Attendance a = new Attendance();
        a.setAttendanceDate(Tables.getLocalDate(record, ATTENDANCE_DATE));
        a.setUserId(record.get(USER_ID));
        a.setAttending(record.get(ATTENDING));
        a.setUpdatedAt(record.get(UPDATED_AT));
        return a;
    }
}
