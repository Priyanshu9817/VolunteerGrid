package com.volunteergrid.service;

import com.volunteergrid.dto.UserDto;
import com.volunteergrid.dto.UserRegistrationDto;
import com.volunteergrid.entity.Role;
import com.volunteergrid.entity.User;
import com.volunteergrid.exception.BusinessRuleException;
import com.volunteergrid.exception.ResourceNotFoundException;
import com.volunteergrid.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles user registration, retrieval, and management.
 *
 * Why @Transactional on write methods?
 * It ensures that all DB operations in a method either ALL succeed
 * or ALL fail — preventing partial state (ACID principle).
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto registerUser(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException(
                    "An account with email " + dto.getEmail() + " already exists.");
        }

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : Role.VOLUNTEER)
                .enabled(true)
                .build();

        return toDto(userRepository.save(user));
    }

    public UserDto findById(Long id) {
        return toDto(getUserOrThrow(id));
    }

    public UserDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        return toDto(user);
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }

    public User getUserEntityById(Long id) {
        return getUserOrThrow(id);
    }

    public List<UserDto> findAll() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<UserDto> findByRole(Role role) {
        return userRepository.findByRole(role).stream().map(this::toDto).toList();
    }

    @Transactional
    public void toggleEnabled(Long userId) {
        User user = getUserOrThrow(userId);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(Long userId, Role newRole) {
        User user = getUserOrThrow(userId);
        user.setRole(newRole);
        userRepository.save(user);
    }

    // ---- Private helpers ----

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}
