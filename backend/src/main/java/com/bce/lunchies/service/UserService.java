package com.bce.lunchies.service;

import com.bce.lunchies.model.AppUser;
import com.bce.lunchies.model.Role;
import com.bce.lunchies.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Value("${super.admin.slack.id:}")
    private String superAdminSlackId;

    public Mono<AppUser> findOrCreate(String slackUserId) {
        return userRepository.findBySlackUserId(slackUserId)
                .switchIfEmpty(Mono.defer(() -> createNewUser(slackUserId)))
                .flatMap(user -> userRepository.updateLastLogin(user.getId()).thenReturn(user));
    }

    public Mono<AppUser> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Flux<AppUser> findAll() {
        return userRepository.findAll();
    }

    public Mono<Void> promoteToAdmin(UUID userId) {
        return userRepository.updateRole(userId, Role.ADMIN);
    }

    public Mono<Void> demoteToUser(UUID userId) {
        return userRepository.updateRole(userId, Role.USER);
    }

    private Mono<AppUser> createNewUser(String slackUserId) {
        Role role = slackUserId.equals(superAdminSlackId) ? Role.SUPER_ADMIN : Role.USER;
        return userRepository.create(slackUserId, null, null, role);
    }
}
