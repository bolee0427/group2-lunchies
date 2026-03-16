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
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestJooqConfig.class)
class MenuRepositoryTest {

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        TestJooqConfig.cleanAllTables(dsl);

        testUser = userRepository.create("UADMIN1", "Admin", "admin@example.com", Role.ADMIN).block();
    }

    @Test
    void create_shouldCreateMenuWithAllFields() {
        LocalDate date = LocalDate.of(2026, 3, 16);

        StepVerifier.create(menuRepository.create(date, "Monday Special", testUser.getId()))
                .assertNext(menu -> {
                    assertThat(menu.getId()).isNotNull();
                    assertThat(menu.getMenuDate()).isEqualTo(date);
                    assertThat(menu.getTitle()).isEqualTo("Monday Special");
                    assertThat(menu.getCreatedBy()).isEqualTo(testUser.getId());
                    assertThat(menu.getSlackMessageTs()).isNull();
                    assertThat(menu.getCreatedAt()).isNotNull();
                    assertThat(menu.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void create_shouldAllowNullTitle() {
        LocalDate date = LocalDate.of(2026, 3, 16);

        StepVerifier.create(menuRepository.create(date, null, testUser.getId()))
                .assertNext(menu -> {
                    assertThat(menu.getTitle()).isNull();
                    assertThat(menu.getMenuDate()).isEqualTo(date);
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnMenuWhenExists() {
        LocalDate date = LocalDate.of(2026, 3, 16);
        Menu created = menuRepository.create(date, "Monday Special", testUser.getId()).block();

        StepVerifier.create(menuRepository.findById(created.getId()))
                .assertNext(menu -> {
                    assertThat(menu.getId()).isEqualTo(created.getId());
                    assertThat(menu.getTitle()).isEqualTo("Monday Special");
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        StepVerifier.create(menuRepository.findById(java.util.UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void findByDate_shouldReturnMenuForDate() {
        LocalDate date = LocalDate.of(2026, 3, 16);
        menuRepository.create(date, "Monday Special", testUser.getId()).block();

        StepVerifier.create(menuRepository.findByDate(date))
                .assertNext(menu -> {
                    assertThat(menu.getMenuDate()).isEqualTo(date);
                    assertThat(menu.getTitle()).isEqualTo("Monday Special");
                })
                .verifyComplete();
    }

    @Test
    void findByDate_shouldReturnEmptyWhenNoMenuForDate() {
        StepVerifier.create(menuRepository.findByDate(LocalDate.of(2099, 1, 1)))
                .verifyComplete();
    }

    @Test
    void findByDateRange_shouldReturnMenusInRange() {
        menuRepository.create(LocalDate.of(2026, 3, 16), "Monday", testUser.getId()).block();
        menuRepository.create(LocalDate.of(2026, 3, 17), "Tuesday", testUser.getId()).block();
        menuRepository.create(LocalDate.of(2026, 3, 18), "Wednesday", testUser.getId()).block();
        menuRepository.create(LocalDate.of(2026, 3, 20), "Friday", testUser.getId()).block();

        StepVerifier.create(menuRepository.findByDateRange(
                        LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 18)))
                .assertNext(menu -> assertThat(menu.getTitle()).isEqualTo("Monday"))
                .assertNext(menu -> assertThat(menu.getTitle()).isEqualTo("Tuesday"))
                .assertNext(menu -> assertThat(menu.getTitle()).isEqualTo("Wednesday"))
                .verifyComplete();
    }

    @Test
    void findByDateRange_shouldReturnEmptyWhenNoMenusInRange() {
        StepVerifier.create(menuRepository.findByDateRange(
                        LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 7)))
                .verifyComplete();
    }

    @Test
    void findByDateRange_shouldReturnMenusOrderedByDate() {
        menuRepository.create(LocalDate.of(2026, 3, 18), "Wednesday", testUser.getId()).block();
        menuRepository.create(LocalDate.of(2026, 3, 16), "Monday", testUser.getId()).block();
        menuRepository.create(LocalDate.of(2026, 3, 17), "Tuesday", testUser.getId()).block();

        StepVerifier.create(menuRepository.findByDateRange(
                        LocalDate.of(2026, 3, 16), LocalDate.of(2026, 3, 18)))
                .assertNext(menu -> assertThat(menu.getMenuDate()).isEqualTo(LocalDate.of(2026, 3, 16)))
                .assertNext(menu -> assertThat(menu.getMenuDate()).isEqualTo(LocalDate.of(2026, 3, 17)))
                .assertNext(menu -> assertThat(menu.getMenuDate()).isEqualTo(LocalDate.of(2026, 3, 18)))
                .verifyComplete();
    }

    @Test
    void updateSlackMessageTs_shouldUpdateTimestamp() {
        Menu created = menuRepository.create(LocalDate.of(2026, 3, 16), "Monday", testUser.getId()).block();

        StepVerifier.create(menuRepository.updateSlackMessageTs(created.getId(), "1234567890.123456"))
                .verifyComplete();

        StepVerifier.create(menuRepository.findById(created.getId()))
                .assertNext(menu -> assertThat(menu.getSlackMessageTs()).isEqualTo("1234567890.123456"))
                .verifyComplete();
    }

    @Test
    void delete_shouldRemoveMenu() {
        Menu created = menuRepository.create(LocalDate.of(2026, 3, 16), "Monday", testUser.getId()).block();

        StepVerifier.create(menuRepository.delete(created.getId()))
                .verifyComplete();

        StepVerifier.create(menuRepository.findById(created.getId()))
                .verifyComplete();
    }
}
