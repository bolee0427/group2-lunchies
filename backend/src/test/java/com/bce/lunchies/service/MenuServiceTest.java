package com.bce.lunchies.service;

import com.bce.lunchies.model.Menu;
import com.bce.lunchies.model.MenuItem;
import com.bce.lunchies.repository.MenuItemRepository;
import com.bce.lunchies.repository.MenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuService menuService;

    private final UUID menuId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void createMenu_shouldDelegateToRepository() {
        Menu menu = new Menu();
        menu.setId(menuId);
        menu.setMenuDate(LocalDate.of(2026, 3, 17));
        when(menuRepository.create(any(), any(), any())).thenReturn(Mono.just(menu));

        StepVerifier.create(menuService.createMenu(LocalDate.of(2026, 3, 17), "Tuesday Lunch", userId))
                .assertNext(m -> assertThat(m.getId()).isEqualTo(menuId))
                .verifyComplete();
    }

    @Test
    void addMenuItem_shouldValidateAndCreate() {
        MenuItem item = makeItem("Pasta", new String[]{"VEGAN"}, new String[]{"GLUTEN"});
        when(menuItemRepository.create(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Mono.just(item));

        StepVerifier.create(menuService.addMenuItem(menuId, "Pasta", "Creamy pasta", 0,
                        new String[]{"VEGAN"}, new String[]{"GLUTEN"}))
                .assertNext(i -> assertThat(i.getName()).isEqualTo("Pasta"))
                .verifyComplete();
    }

    @Test
    void addMenuItem_shouldRejectInvalidTag() {
        StepVerifier.create(menuService.addMenuItem(menuId, "Pasta", "desc", 0,
                        new String[]{"INVALID_TAG"}, new String[]{}))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Invalid dietary tag"))
                .verify();

        verifyNoInteractions(menuItemRepository);
    }

    @Test
    void addMenuItem_shouldRejectInvalidAllergen() {
        StepVerifier.create(menuService.addMenuItem(menuId, "Pasta", "desc", 0,
                        new String[]{"VEGAN"}, new String[]{"FAKE_ALLERGEN"}))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Invalid allergen"))
                .verify();
    }

    @Test
    void addMenuItem_shouldAllowNullTagsAndAllergens() {
        MenuItem item = makeItem("Salad", null, null);
        when(menuItemRepository.create(any(), any(), any(), anyInt(), isNull(), isNull()))
                .thenReturn(Mono.just(item));

        StepVerifier.create(menuService.addMenuItem(menuId, "Salad", "Fresh salad", 0, null, null))
                .assertNext(i -> assertThat(i.getName()).isEqualTo("Salad"))
                .verifyComplete();
    }

    @Test
    void replaceMenuItems_shouldDeleteThenInsert() {
        when(menuItemRepository.deleteByMenuId(menuId)).thenReturn(Mono.empty());
        when(menuItemRepository.create(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Mono.just(makeItem("Item", new String[]{}, new String[]{})));

        List<MenuService.MenuItemInput> items = List.of(
                new MenuService.MenuItemInput("Soup", "Tomato soup", new String[]{"VEGAN"}, new String[]{}),
                new MenuService.MenuItemInput("Bread", "Sourdough", new String[]{}, new String[]{"GLUTEN"})
        );

        StepVerifier.create(menuService.replaceMenuItems(menuId, items))
                .verifyComplete();

        verify(menuItemRepository).deleteByMenuId(menuId);
        verify(menuItemRepository, times(2)).create(eq(menuId), any(), any(), anyInt(), any(), any());
    }

    @Test
    void deleteMenu_shouldDelegateToRepository() {
        when(menuRepository.delete(menuId)).thenReturn(Mono.empty());

        StepVerifier.create(menuService.deleteMenu(menuId))
                .verifyComplete();

        verify(menuRepository).delete(menuId);
    }

    @Test
    void findByDate_shouldDelegateToRepository() {
        Menu menu = new Menu();
        menu.setId(menuId);
        LocalDate date = LocalDate.of(2026, 3, 17);
        when(menuRepository.findByDate(date)).thenReturn(Mono.just(menu));

        StepVerifier.create(menuService.findByDate(date))
                .assertNext(m -> assertThat(m.getId()).isEqualTo(menuId))
                .verifyComplete();
    }

    @Test
    void findByDateRange_shouldDelegateToRepository() {
        when(menuRepository.findByDateRange(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(menuService.findByDateRange(LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 20)))
                .verifyComplete();
    }

    private MenuItem makeItem(String name, String[] tags, String[] allergens) {
        MenuItem item = new MenuItem();
        item.setId(UUID.randomUUID());
        item.setMenuId(menuId);
        item.setName(name);
        item.setTags(tags);
        item.setAllergens(allergens);
        return item;
    }
}
