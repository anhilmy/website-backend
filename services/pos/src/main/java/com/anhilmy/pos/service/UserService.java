package com.anhilmy.pos.service;

import java.util.Arrays;
import java.util.List;

import com.anhilmy.pos.model.User;

public class UserService {
    public List<User> getAllUsers() {
        return Arrays.asList(new User(1, "Alice"), new User(2, "Bob"));
    }

    public User getUserById(int id) {
        return new User(id, "Mock User");
    }
}