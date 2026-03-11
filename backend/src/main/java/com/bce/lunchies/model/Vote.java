package com.bce.lunchies.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class Vote {
    private UUID menuId;
    private UUID userId;
    private short stars;
    private OffsetDateTime updatedAt;
}
