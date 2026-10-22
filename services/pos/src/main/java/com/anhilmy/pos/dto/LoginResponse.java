package com.anhilmy.pos.dto;

/** Successful login response. */
public class LoginResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final long expiresIn;
    private final Long userId;
    private final String username;
    private final String role;

    public LoginResponse(String token, long expiresIn, Long userId, String username, String role) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
