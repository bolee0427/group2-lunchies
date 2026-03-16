package com.bce.lunchies.repository;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.bce.lunchies.TestJooqConfig;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestJooqConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        TestJooqConfig.cleanAllTables(dsl);
    }

    @Test
    void create_shouldCreateUserWithAllFields() {
        StepVerifier.create(userRepository.create("U123", "Alice", "alice@example.com", Role.USER))
                .assertNext(user -> {
                    assertThat(user.getId()).isNotNull();
                    assertThat(user.getSlackUserId()).isEqualTo("U123");
                    assertThat(user.getDisplayName()).isEqualTo("Alice");
                    assertThat(user.getEmail()).isEqualTo("alice@example.com");
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                    assertThat(user.getCreatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void create_shouldCreateAdminUser() {
        StepVerifier.create(userRepository.create("U456", "Bob", "bob@example.com", Role.ADMIN))
                .assertNext(user -> {
                    assertThat(user.getRole()).isEqualTo(Role.ADMIN);
                })
                .verifyComplete();
    }

    @Test
    void findBySlackUserId_shouldReturnUserWhenExists() {
        AppUser created = userRepository.create("U123", "Alice", "alice@example.com", Role.USER).block();

        StepVerifier.create(userRepository.findBySlackUserId("U123"))
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(created.getId());
                    assertThat(user.getSlackUserId()).isEqualTo("U123");
                })
                .verifyComplete();
    }

    @Test
    void findBySlackUserId_shouldReturnEmptyWhenNotExists() {
        StepVerifier.create(userRepository.findBySlackUserId("NONEXISTENT"))
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnUserWhenExists() {
        AppUser created = userRepository.create("U123", "Alice", "alice@example.com", Role.USER).block();

        StepVerifier.create(userRepository.findById(created.getId()))
                .assertNext(user -> {
                    assertThat(user.getSlackUserId()).isEqualTo("U123");
                    assertThat(user.getDisplayName()).isEqualTo("Alice");
                })
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        StepVerifier.create(userRepository.findById(java.util.UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnEmptyWhenNoUsers() {
        StepVerifier.create(userRepository.findAll())
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllUsersOrderedByDisplayName() {
        userRepository.create("U1", "Charlie", "charlie@example.com", Role.USER).block();
        userRepository.create("U2", "Alice", "alice@example.com", Role.ADMIN).block();
        userRepository.create("U3", "Bob", "bob@example.com", Role.USER).block();

        StepVerifier.create(userRepository.findAll())
                .assertNext(user -> assertThat(user.getDisplayName()).isEqualTo("Alice"))
                .assertNext(user -> assertThat(user.getDisplayName()).isEqualTo("Bob"))
                .assertNext(user -> assertThat(user.getDisplayName()).isEqualTo("Charlie"))
                .verifyComplete();
    }

    @Test
    void updateRole_shouldChangeUserRole() {
        AppUser created = userRepository.create("U123", "Alice", "alice@example.com", Role.USER).block();

        StepVerifier.create(userRepository.updateRole(created.getId(), Role.ADMIN))
                .verifyComplete();

        StepVerifier.create(userRepository.findById(created.getId()))
                .assertNext(user -> assertThat(user.getRole()).isEqualTo(Role.ADMIN))
                .verifyComplete();
    }

    @Test
    void updateRole_shouldPromoteToSuperAdmin() {
        AppUser created = userRepository.create("U123", "Alice", "alice@example.com", Role.USER).block();

        StepVerifier.create(userRepository.updateRole(created.getId(), Role.SUPER_ADMIN))
                .verifyComplete();

        StepVerifier.create(userRepository.findById(created.getId()))
                .assertNext(user -> assertThat(user.getRole()).isEqualTo(Role.SUPER_ADMIN))
                .verifyComplete();
    }

    @Test
    void updateLastLogin_shouldUpdateTimestamp() {
        AppUser created = userRepository.create("U123", "Alice", "alice@example.com", Role.USER).block();

        StepVerifier.create(userRepository.updateLastLogin(created.getId()))
                .verifyComplete();

        StepVerifier.create(userRepository.findById(created.getId()))
                .assertNext(user -> assertThat(user.getLastLogin()).isNotNull())
                .verifyComplete();
    }
}
