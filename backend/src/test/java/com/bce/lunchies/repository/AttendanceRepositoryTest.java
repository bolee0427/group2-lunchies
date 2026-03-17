package com.bce.lunchies.repository;

import com.bce.lunchies.TestJooqConfig;
import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestJooqConfig.class)
class AttendanceRepositoryTest {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    private AppUser testUser;
    private AppUser testUser2;
    private LocalDate nextMonday;

    @BeforeEach
    void setUp() {
        TestJooqConfig.cleanAllTables(dsl);

        testUser = userRepository.create("U001", "Alice", "alice@example.com", Role.USER).block();
        testUser2 = userRepository.create("U002", "Bob", "bob@example.com", Role.USER).block();
        nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    @Test
    void upsert_shouldInsertNewAttendance() {
        StepVerifier.create(attendanceRepository.upsert(nextMonday, testUser.getId(), true))
                .verifyComplete();

        StepVerifier.create(attendanceRepository.findByDateAndUserId(nextMonday, testUser.getId()))
                .assertNext(a -> {
                    assertThat(a.getAttendanceDate()).isEqualTo(nextMonday);
                    assertThat(a.getUserId()).isEqualTo(testUser.getId());
                    assertThat(a.isAttending()).isTrue();
                    assertThat(a.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void upsert_shouldUpdateExistingAttendance() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();

        StepVerifier.create(attendanceRepository.upsert(nextMonday, testUser.getId(), false))
                .verifyComplete();

        StepVerifier.create(attendanceRepository.findByDateAndUserId(nextMonday, testUser.getId()))
                .assertNext(a -> assertThat(a.isAttending()).isFalse())
                .verifyComplete();
    }

    @Test
    void upsert_shouldAllowDifferentUsersOnSameDate() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(nextMonday, testUser2.getId(), false).block();

        StepVerifier.create(attendanceRepository.findByDate(nextMonday))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByDateAndUserId_shouldReturnEmptyWhenNotExists() {
        StepVerifier.create(attendanceRepository.findByDateAndUserId(nextMonday, testUser.getId()))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndDateRange_shouldReturnWeekdayAttendance() {
        LocalDate tuesday = nextMonday.plusDays(1);
        LocalDate wednesday = nextMonday.plusDays(2);
        LocalDate friday = nextMonday.plusDays(4);

        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(tuesday, testUser.getId(), true).block();
        attendanceRepository.upsert(wednesday, testUser.getId(), false).block();
        attendanceRepository.upsert(friday, testUser.getId(), true).block();

        StepVerifier.create(attendanceRepository.findByUserIdAndDateRange(
                        testUser.getId(), nextMonday, friday))
                .assertNext(a -> {
                    assertThat(a.getAttendanceDate()).isEqualTo(nextMonday);
                    assertThat(a.isAttending()).isTrue();
                })
                .assertNext(a -> {
                    assertThat(a.getAttendanceDate()).isEqualTo(tuesday);
                    assertThat(a.isAttending()).isTrue();
                })
                .assertNext(a -> {
                    assertThat(a.getAttendanceDate()).isEqualTo(wednesday);
                    assertThat(a.isAttending()).isFalse();
                })
                .assertNext(a -> {
                    assertThat(a.getAttendanceDate()).isEqualTo(friday);
                    assertThat(a.isAttending()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findByUserIdAndDateRange_shouldReturnEmptyWhenNoRecords() {
        StepVerifier.create(attendanceRepository.findByUserIdAndDateRange(
                        testUser.getId(), nextMonday, nextMonday.plusDays(4)))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndDateRange_shouldOnlyReturnSpecifiedUser() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(nextMonday, testUser2.getId(), false).block();

        StepVerifier.create(attendanceRepository.findByUserIdAndDateRange(
                        testUser.getId(), nextMonday, nextMonday))
                .assertNext(a -> {
                    assertThat(a.getUserId()).isEqualTo(testUser.getId());
                    assertThat(a.isAttending()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void countAttendingByDate_shouldCountOnlyYes() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(nextMonday, testUser2.getId(), false).block();

        StepVerifier.create(attendanceRepository.countAttendingByDate(nextMonday))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void countAttendingByDate_shouldReturnZeroWhenNoRecords() {
        StepVerifier.create(attendanceRepository.countAttendingByDate(nextMonday))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void countAttendingByDate_shouldCountAllAttending() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(nextMonday, testUser2.getId(), true).block();

        StepVerifier.create(attendanceRepository.countAttendingByDate(nextMonday))
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();
    }

    @Test
    void findByDate_shouldReturnAllRecordsForDate() {
        attendanceRepository.upsert(nextMonday, testUser.getId(), true).block();
        attendanceRepository.upsert(nextMonday, testUser2.getId(), false).block();

        StepVerifier.create(attendanceRepository.findByDate(nextMonday))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByDate_shouldReturnEmptyForDateWithNoRecords() {
        StepVerifier.create(attendanceRepository.findByDate(nextMonday))
                .verifyComplete();
    }
}
