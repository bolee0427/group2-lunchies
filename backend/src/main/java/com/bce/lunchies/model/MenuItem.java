package com.bce.lunchies.model;

import lombok.Data;

import java.util.UUID;

@Data
public class MenuItem {
    private UUID id;
    private UUID menuId;
    private String name;
    private String description;
    private int sortOrder;
    private String[] tags;
    private String[] allergens;
}
