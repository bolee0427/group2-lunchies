package com.bce.lunchies.service;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import com.bce.lunchies.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private static final String SUPER_ADMIN_SLACK_ID = "U_SUPER";

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        // Set the superAdminSlackId via reflection since it's a @Value field
        try {
            var field = UserService.class.getDeclaredField("superAdminSlackId");
            field.setAccessible(true);
            field.set(userService, SUPER_ADMIN_SLACK_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findOrCreate_shouldReturnExistingUser() {
        AppUser existing = makeUser("U123", Role.USER);
        when(userRepository.findBySlackUserId("U123")).thenReturn(Mono.just(existing));
        when(userRepository.updateLastLogin(existing.getId())).thenReturn(Mono.empty());

        StepVerifier.create(userService.findOrCreate("U123"))
                .assertNext(user -> {
                    assertThat(user.getSlackUserId()).isEqualTo("U123");
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                })
                .verifyComplete();

        verify(userRepository, never()).create(any(), any(), any(), any());
        verify(userRepository).updateLastLogin(existing.getId());
    }

    @Test
    void findOrCreate_shouldCreateNewUserWhenNotExists() {
        AppUser newUser = makeUser("U999", Role.USER);
        when(userRepository.findBySlackUserId("U999")).thenReturn(Mono.empty());
        when(userRepository.create("U999", null, null, Role.USER)).thenReturn(Mono.just(newUser));
        when(userRepository.updateLastLogin(newUser.getId())).thenReturn(Mono.empty());

        StepVerifier.create(userService.findOrCreate("U999"))
                .assertNext(user -> {
                    assertThat(user.getSlackUserId()).isEqualTo("U999");
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                })
                .verifyComplete();

        verify(userRepository).create("U999", null, null, Role.USER);
    }

    @Test
    void findOrCreate_shouldCreateSuperAdminWhenSlackIdMatches() {
        AppUser superAdmin = makeUser(SUPER_ADMIN_SLACK_ID, Role.SUPER_ADMIN);
        when(userRepository.findBySlackUserId(SUPER_ADMIN_SLACK_ID)).thenReturn(Mono.empty());
        when(userRepository.create(SUPER_ADMIN_SLACK_ID, null, null, Role.SUPER_ADMIN)).thenReturn(Mono.just(superAdmin));
        when(userRepository.updateLastLogin(superAdmin.getId())).thenReturn(Mono.empty());

        StepVerifier.create(userService.findOrCreate(SUPER_ADMIN_SLACK_ID))
                .assertNext(user -> {
                    assertThat(user.getRole()).isEqualTo(Role.SUPER_ADMIN);
                })
                .verifyComplete();

        verify(userRepository).create(SUPER_ADMIN_SLACK_ID, null, null, Role.SUPER_ADMIN);
    }

    @Test
    void findById_shouldReturnUser() {
        AppUser user = makeUser("U123", Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        StepVerifier.create(userService.findById(user.getId()))
                .assertNext(u -> assertThat(u.getId()).isEqualTo(user.getId()))
                .verifyComplete();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        AppUser user1 = makeUser("U1", Role.USER);
        AppUser user2 = makeUser("U2", Role.ADMIN);
        when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));

        StepVerifier.create(userService.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void promoteToAdmin_shouldCallUpdateRole() {
        UUID userId = UUID.randomUUID();
        when(userRepository.updateRole(userId, Role.ADMIN)).thenReturn(Mono.empty());

        StepVerifier.create(userService.promoteToAdmin(userId))
                .verifyComplete();

        verify(userRepository).updateRole(userId, Role.ADMIN);
    }

    @Test
    void demoteToUser_shouldCallUpdateRole() {
        UUID userId = UUID.randomUUID();
        when(userRepository.updateRole(userId, Role.USER)).thenReturn(Mono.empty());

        StepVerifier.create(userService.demoteToUser(userId))
                .verifyComplete();

        verify(userRepository).updateRole(userId, Role.USER);
    }

    private AppUser makeUser(String slackUserId, Role role) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setSlackUserId(slackUserId);
        user.setRole(role);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
