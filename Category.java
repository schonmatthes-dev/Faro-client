package com.faro.client.module;

public enum Category {
    CAMERA("Camera"),       // Freecam, Freelook
    ESP("ESP"),             // Entity ESP, Block ESP, NameTags
    FINDER("Finder"),       // Chunk Finder, Block Notifier
    FAVORITES("Favorites"); // virtuell: alle mit Stern

    public final String label;
    Category(String label) { this.label = label; }
}
