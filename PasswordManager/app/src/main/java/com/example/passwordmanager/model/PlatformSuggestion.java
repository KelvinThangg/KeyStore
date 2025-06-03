package com.example.passwordmanager.model; // Hoặc package chính của bạn

public class PlatformSuggestion {
    private String name;
    private int logoResId; // ID tài nguyên drawable cho logo
    private String websiteUrl;

    public PlatformSuggestion(String name, int logoResId, String websiteUrl) {
        this.name = name;
        this.logoResId = logoResId;
        this.websiteUrl = websiteUrl;
    }

    public String getName() {
        return name;
    }

    public int getLogoResId() {
        return logoResId;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    // Quan trọng: Override toString() để AutoCompleteTextView hiển thị tên
    @Override
    public String toString() {
        return name;
    }
}
