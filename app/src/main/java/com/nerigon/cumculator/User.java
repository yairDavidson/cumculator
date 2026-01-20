package com.nerigon.cumculator;

public class User {
    public String password;
    public long createdAt;

    public User() {} // REQUIRED

    public User(String password) {
        this.password = password;
        this.createdAt = System.currentTimeMillis();
    }
}
