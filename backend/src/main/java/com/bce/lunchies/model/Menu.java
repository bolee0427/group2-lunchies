package com.bce.lunchies.model;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class Menu {
    private UUID id;
    private LocalDate menuDate;
    private String title;
    private UUID createdBy;
    private String slackMessageTs;
    private Instant createdAt;
    private Instant updatedAt;
}
