package com.bce.lunchies.service;

import com.bce.lunchies.model.Attendance;
import com.bce.lunchies.repository.AttendanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    private AttendanceService attendanceService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository);
        try {
            var field = AttendanceService.class.getDeclaredField("lookaheadDays");
            field.setAccessible(true);
            field.set(attendanceService, 7);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void rsvp_shouldSucceedForFutureWeekday() {
        LocalDate nextWeekday = nextWeekday();
        when(attendanceRepository.upsert(nextWeekday, userId, true)).thenReturn(Mono.empty());

        StepVerifier.create(attendanceService.rsvp(userId, nextWeekday, true))
                .verifyComplete();

        verify(attendanceRepository).upsert(nextWeekday, userId, true);
    }

    @Test
    void rsvp_shouldRejectPastDates() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        StepVerifier.create(attendanceService.rsvp(userId, yesterday, true))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("today or past"))
                .verify();

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void rsvp_shouldRejectToday() {
        LocalDate today = LocalDate.now();

        StepVerifier.create(attendanceService.rsvp(userId, today, true))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("today or past"))
                .verify();

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void rsvp_shouldRejectWeekends() {
        LocalDate nextSaturday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY));

        StepVerifier.create(attendanceService.rsvp(userId, nextSaturday, true))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("weekends"))
                .verify();

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void rsvp_shouldRejectDateBeyondLookahead() {
        LocalDate tooFar = nextWeekday().plusDays(30);

        StepVerifier.create(attendanceService.rsvp(userId, tooFar, true))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("days ahead"))
                .verify();

        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void getHeadcount_shouldReturnCount() {
        LocalDate date = LocalDate.now();
        when(attendanceRepository.countAttendingByDate(date)).thenReturn(Mono.just(15));

        StepVerifier.create(attendanceService.getHeadcount(date))
                .expectNext(15)
                .verifyComplete();
    }

    @Test
    void getUpcomingAttendance_shouldQueryCorrectDateRange() {
        when(attendanceRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
                .thenReturn(Flux.empty());

        StepVerifier.create(attendanceService.getUpcomingAttendance(userId))
                .verifyComplete();

        verify(attendanceRepository).findByUserIdAndDateRange(
                eq(userId),
                eq(LocalDate.now().plusDays(1)),
                eq(LocalDate.now().plusDays(7))
        );
    }

    @Test
    void getAttendanceByDate_shouldDelegateToRepository() {
        LocalDate date = LocalDate.now();
        Attendance a = new Attendance();
        a.setAttendanceDate(date);
        a.setUserId(userId);
        a.setAttending(true);
        when(attendanceRepository.findByDate(date)).thenReturn(Flux.just(a));

        StepVerifier.create(attendanceService.getAttendanceByDate(date))
                .expectNextCount(1)
                .verifyComplete();
    }

    private LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
