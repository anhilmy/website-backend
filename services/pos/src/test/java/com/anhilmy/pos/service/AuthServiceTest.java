package com.anhilmy.pos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anhilmy.pos.dto.LoginResponse;
import com.anhilmy.pos.exception.AuthenticationException;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.repository.UserRepository;
import com.anhilmy.pos.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository);
    }

    private User user(String username, String rawPassword, String role) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.hash(rawPassword));
        u.setRole(role);
        return u;
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(user("cashier", "secret123", "CASHIER")));

        LoginResponse response = service.login("cashier", "secret123");
        assertNotNull(response.getToken());
        assertEquals("CASHIER", response.getRole());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("cashier"))
                .thenReturn(Optional.of(user("cashier", "secret123", "CASHIER")));

        assertThrows(AuthenticationException.class, () -> service.login("cashier", "wrong"));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(AuthenticationException.class, () -> service.login("ghost", "whatever1"));
    }
}
