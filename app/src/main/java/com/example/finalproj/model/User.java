package com.example.finalproj.model;

public class User {
    private String firstName;
    private String lastName;
    private String username;
    private String email;

    public User() {
    }

    public User(String firstName, String lastName, String username, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
    }
}
