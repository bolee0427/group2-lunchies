package com.bce.lunchies.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class Menu {
    private UUID id;
    private LocalDate menuDate;
    private String title;
    private UUID createdBy;
    private String slackMessageTs;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
