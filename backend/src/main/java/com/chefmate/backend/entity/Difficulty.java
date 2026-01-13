package com.chefmate.backend.entity;

public enum Difficulty {
    EASY("Лесно"),
    MEDIUM("Средно"),
    HARD("Трудно");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}