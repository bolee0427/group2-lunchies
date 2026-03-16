package com.bce.lunchies.repository;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Menu;
import com.bce.lunchies.model.Role;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.bce.lunchies.TestJooqConfig;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestJooqConfig.class)
class MenuItemRepositoryTest {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    private Menu testMenu;

    @BeforeEach
    void setUp() {
        TestJooqConfig.cleanAllTables(dsl);

        AppUser user = userRepository.create("UADMIN1", "Admin", "admin@example.com", Role.ADMIN).block();
        testMenu = menuRepository.create(LocalDate.of(2026, 3, 16), "Monday Special", user.getId()).block();
    }

    @Test
    void create_shouldCreateItemWithAllFields() {
        String[] tags = {"VEGAN", "GLUTEN_FREE"};
        String[] allergens = {"SOY"};

        StepVerifier.create(menuItemRepository.create(testMenu.getId(), "Tofu Bowl", "Crispy tofu with rice", 0, tags, allergens))
                .assertNext(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getMenuId()).isEqualTo(testMenu.getId());
                    assertThat(item.getName()).isEqualTo("Tofu Bowl");
                    assertThat(item.getDescription()).isEqualTo("Crispy tofu with rice");
                    assertThat(item.getSortOrder()).isEqualTo(0);
                    assertThat(item.getTags()).containsExactly("VEGAN", "GLUTEN_FREE");
                    assertThat(item.getAllergens()).containsExactly("SOY");
                })
                .verifyComplete();
    }

    @Test
    void create_shouldCreateItemWithEmptyTagsAndAllergens() {
        StepVerifier.create(menuItemRepository.create(testMenu.getId(), "Plain Salad", "Mixed greens", 0, new String[]{}, new String[]{}))
                .assertNext(item -> {
                    assertThat(item.getTags()).isEmpty();
                    assertThat(item.getAllergens()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void create_shouldCreateItemWithNullDescription() {
        StepVerifier.create(menuItemRepository.create(testMenu.getId(), "Mystery Dish", null, 0, new String[]{}, new String[]{}))
                .assertNext(item -> {
                    assertThat(item.getName()).isEqualTo("Mystery Dish");
                    assertThat(item.getDescription()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void findByMenuId_shouldReturnItemsOrderedBySortOrder() {
        menuItemRepository.create(testMenu.getId(), "Dessert", "Cake", 2, new String[]{}, new String[]{}).block();
        menuItemRepository.create(testMenu.getId(), "Starter", "Soup", 0, new String[]{}, new String[]{}).block();
        menuItemRepository.create(testMenu.getId(), "Main", "Pasta", 1, new String[]{}, new String[]{}).block();

        StepVerifier.create(menuItemRepository.findByMenuId(testMenu.getId()))
                .assertNext(item -> {
                    assertThat(item.getName()).isEqualTo("Starter");
                    assertThat(item.getSortOrder()).isEqualTo(0);
                })
                .assertNext(item -> {
                    assertThat(item.getName()).isEqualTo("Main");
                    assertThat(item.getSortOrder()).isEqualTo(1);
                })
                .assertNext(item -> {
                    assertThat(item.getName()).isEqualTo("Dessert");
                    assertThat(item.getSortOrder()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    void findByMenuId_shouldReturnEmptyWhenNoItems() {
        StepVerifier.create(menuItemRepository.findByMenuId(testMenu.getId()))
                .verifyComplete();
    }

    @Test
    void findByMenuId_shouldReturnEmptyForNonexistentMenu() {
        StepVerifier.create(menuItemRepository.findByMenuId(java.util.UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void findByMenuId_shouldOnlyReturnItemsForSpecifiedMenu() {
        AppUser user = userRepository.findBySlackUserId("UADMIN1").block();
        Menu otherMenu = menuRepository.create(LocalDate.of(2026, 3, 17), "Tuesday", user.getId()).block();

        menuItemRepository.create(testMenu.getId(), "Monday Dish", "desc", 0, new String[]{}, new String[]{}).block();
        menuItemRepository.create(otherMenu.getId(), "Tuesday Dish", "desc", 0, new String[]{}, new String[]{}).block();

        StepVerifier.create(menuItemRepository.findByMenuId(testMenu.getId()))
                .assertNext(item -> assertThat(item.getName()).isEqualTo("Monday Dish"))
                .verifyComplete();
    }

    @Test
    void deleteByMenuId_shouldDeleteAllItemsForMenu() {
        menuItemRepository.create(testMenu.getId(), "Item 1", "desc", 0, new String[]{}, new String[]{}).block();
        menuItemRepository.create(testMenu.getId(), "Item 2", "desc", 1, new String[]{}, new String[]{}).block();

        StepVerifier.create(menuItemRepository.deleteByMenuId(testMenu.getId()))
                .verifyComplete();

        StepVerifier.create(menuItemRepository.findByMenuId(testMenu.getId()))
                .verifyComplete();
    }

    @Test
    void deleteByMenuId_shouldNotAffectOtherMenuItems() {
        AppUser user = userRepository.findBySlackUserId("UADMIN1").block();
        Menu otherMenu = menuRepository.create(LocalDate.of(2026, 3, 17), "Tuesday", user.getId()).block();

        menuItemRepository.create(testMenu.getId(), "Monday Dish", "desc", 0, new String[]{}, new String[]{}).block();
        menuItemRepository.create(otherMenu.getId(), "Tuesday Dish", "desc", 0, new String[]{}, new String[]{}).block();

        StepVerifier.create(menuItemRepository.deleteByMenuId(testMenu.getId()))
                .verifyComplete();

        StepVerifier.create(menuItemRepository.findByMenuId(otherMenu.getId()))
                .assertNext(item -> assertThat(item.getName()).isEqualTo("Tuesday Dish"))
                .verifyComplete();
    }
}
