package com.bce.lunchies.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class Attendance {
    private LocalDate attendanceDate;
    private UUID userId;
    private boolean attending;
    private OffsetDateTime updatedAt;
}
