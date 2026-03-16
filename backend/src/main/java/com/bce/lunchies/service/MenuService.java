package com.bce.lunchies.service;

import com.bce.lunchies.model.Allergen;
import com.bce.lunchies.model.DietaryTag;
import com.bce.lunchies.model.Menu;
import com.bce.lunchies.model.MenuItem;
import com.bce.lunchies.repository.MenuItemRepository;
import com.bce.lunchies.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;

    public Mono<Menu> findById(UUID id) {
        return menuRepository.findById(id);
    }

    public Mono<Menu> findByDate(LocalDate date) {
        return menuRepository.findByDate(date);
    }

    public Flux<Menu> findByDateRange(LocalDate from, LocalDate to) {
        return menuRepository.findByDateRange(from, to);
    }

    public Flux<MenuItem> getMenuItems(UUID menuId) {
        return menuItemRepository.findByMenuId(menuId);
    }

    public Mono<Menu> createMenu(LocalDate menuDate, String title, UUID createdBy) {
        return menuRepository.create(menuDate, title, createdBy);
    }

    public Mono<MenuItem> addMenuItem(UUID menuId, String name, String description,
                                       int sortOrder, String[] tags, String[] allergens) {
        return validateTags(tags)
                .then(Mono.defer(() -> validateAllergens(allergens)))
                .then(Mono.defer(() -> menuItemRepository.create(menuId, name, description, sortOrder, tags, allergens)));
    }

    public Mono<Void> replaceMenuItems(UUID menuId, List<MenuItemInput> items) {
        return menuItemRepository.deleteByMenuId(menuId)
                .then(Flux.fromIterable(items)
                        .index()
                        .flatMap(indexed -> {
                            MenuItemInput item = indexed.getT2();
                            int order = indexed.getT1().intValue();
                            return addMenuItem(menuId, item.name(), item.description(),
                                    order, item.tags(), item.allergens());
                        })
                        .then());
    }

    public Mono<Void> updateSlackMessageTs(UUID menuId, String slackMessageTs) {
        return menuRepository.updateSlackMessageTs(menuId, slackMessageTs);
    }

    public Mono<Void> deleteMenu(UUID id) {
        return menuRepository.delete(id);
    }

    private Mono<Void> validateTags(String[] tags) {
        if (tags == null) return Mono.empty();
        try {
            Arrays.stream(tags).forEach(DietaryTag::valueOf);
            return Mono.empty();
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid dietary tag: " + e.getMessage()));
        }
    }

    private Mono<Void> validateAllergens(String[] allergens) {
        if (allergens == null) return Mono.empty();
        try {
            Arrays.stream(allergens).forEach(Allergen::valueOf);
            return Mono.empty();
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid allergen: " + e.getMessage()));
        }
    }

    public record MenuItemInput(String name, String description, String[] tags, String[] allergens) {}
}
