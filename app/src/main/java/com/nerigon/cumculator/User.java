package com.nerigon.cumculator;

import java.util.List;

//User.java
public class User {
    public String password;
    public long createdAt;
    public int year, month, day, hour, minute, second;
    public List<String> friends;
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
    public void addFriend(String username) {
        if (!friends.contains(username)) {
            friends.add(username);
        }
    }
    public void removeFriend(String username) {
        friends.remove(username);
    }
}