package com.bce.lunchies.model;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AppUser {
    private UUID id;
    private String slackUserId;
    private String email;
    private String displayName;
    private Role role;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;
}
