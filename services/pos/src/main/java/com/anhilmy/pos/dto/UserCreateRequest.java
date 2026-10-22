package com.anhilmy.pos.dto;

/** Payload for creating a new user (manager only). */
public class UserCreateRequest {
    public String username;
    public String password;
    public String role;
}
