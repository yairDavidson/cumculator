package com.nerigon.cumculator;

public class User {
    public String password;
    public long createdAt;
    public int year, month, day, hour, minute, second;

    public User() {} // Required for Firebase

    public User(String password) {
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        // Defaults to 0, or you can set current time here if preferred
    }

    public User(String password, int y, int m, int d, int h, int min, int s) {
        this.password = password;
        this.createdAt = System.currentTimeMillis();
        this.year = y;
        this.month = m;
        this.day = d;
        this.hour = h;
        this.minute = min;
        this.second = s;
    }
}