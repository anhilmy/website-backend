package com.anhilmy.pos.service;

import java.time.LocalDateTime;
import java.util.List;

import com.anhilmy.pos.exception.EntityNotFoundException;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.InputValidator;

/** User management and authentication-related user operations. */
public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this(new UserRepository());
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
    }

    public User createUser(String username, String rawPassword, String role) {
        InputValidator.requireUsername(username);
        InputValidator.requireStrongPassword(rawPassword);
        InputValidator.requireNonBlank(role, "role");
        if (userRepository.existsByUsername(username)) {
            throw new com.anhilmy.pos.exception.BusinessRuleException(
                    "Username already taken: " + username);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(com.anhilmy.pos.util.PasswordUtil.hash(rawPassword));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!com.anhilmy.pos.util.PasswordUtil.matches(oldPassword, user.getPasswordHash())) {
            throw new com.anhilmy.pos.exception.AuthenticationException(
                    "Current password is incorrect");
        }
        InputValidator.requireStrongPassword(newPassword);
        user.setPasswordHash(com.anhilmy.pos.util.PasswordUtil.hash(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.update(user);
    }
}
