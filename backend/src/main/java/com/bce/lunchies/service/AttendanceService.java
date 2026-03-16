package com.bce.lunchies.service;

import com.bce.lunchies.model.Attendance;
import com.bce.lunchies.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Value("${attendance.lookahead-days:7}")
    private int lookaheadDays;

    public Mono<Void> rsvp(UUID userId, LocalDate date, boolean attending) {
        return validateDate(date)
                .then(Mono.defer(() -> attendanceRepository.upsert(date, userId, attending)));
    }

    public Flux<Attendance> getUpcomingAttendance(UUID userId) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(lookaheadDays);
        return attendanceRepository.findByUserIdAndDateRange(userId, tomorrow, end);
    }

    public Mono<Integer> getHeadcount(LocalDate date) {
        return attendanceRepository.countAttendingByDate(date);
    }

    public Flux<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }

    private Mono<Void> validateDate(LocalDate date) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate maxDate = LocalDate.now().plusDays(lookaheadDays);

        if (date.isBefore(tomorrow)) {
            return Mono.error(new IllegalArgumentException("Cannot RSVP for today or past dates"));
        }
        if (date.isAfter(maxDate)) {
            return Mono.error(new IllegalArgumentException("Cannot RSVP more than " + lookaheadDays + " days ahead"));
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Mono.error(new IllegalArgumentException("Cannot RSVP for weekends"));
        }
        return Mono.empty();
    }
}
