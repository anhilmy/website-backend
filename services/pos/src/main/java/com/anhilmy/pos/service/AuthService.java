package com.anhilmy.pos.service;

import com.anhilmy.pos.dto.LoginResponse;
import com.anhilmy.pos.exception.AuthenticationException;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.JWTUtil;
import com.anhilmy.pos.util.PasswordUtil;

/** Authentication: login, logout and session lookup. */
public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this(new UserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(String username, String rawPassword) {
        InputValidator.requireNonBlank(username, "username");
        InputValidator.requireNonBlank(rawPassword, "password");

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!PasswordUtil.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }

        String token = JWTUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, JWTUtil.getExpirationSeconds(), user.getId(),
                user.getUsername(), user.getRole());
    }

    public void logout(String token) {
        JWTUtil.revoke(token);
    }

    public User currentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("Session user no longer exists"));
    }
}
