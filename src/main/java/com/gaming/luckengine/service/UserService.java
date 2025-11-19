package com.gaming.luckengine.service;

import com.gaming.luckengine.domain.entity.User;
import com.gaming.luckengine.exception.ResourceNotFoundException;
import com.gaming.luckengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing User operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user or get existing user by username.
     */
    @Transactional
    public User getOrCreateUser(String username, String email, String fullName) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    log.info("Creating new user: {}", username);
                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .fullName(fullName)
                            .isActive(true)
                            .build();
                    return userRepository.save(user);
                });
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Get user by username.
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * Get all users.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Update user's last played timestamp.
     */
    @Transactional
    public User updateLastPlayed(Long userId) {
        User user = getUserById(userId);
        user.updateLastPlayed();
        return userRepository.save(user);
    }

    /**
     * Toggle user status.
     */
    @Transactional
    public User toggleUserStatus(Long userId, boolean isActive) {
        User user = getUserById(userId);
        user.setIsActive(isActive);
        return userRepository.save(user);
    }
}

