package com.example.passwordmanager.dashboard;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class PasswordItem {
    @Exclude private String id; //
    private String platformName;
    private String username;
    private String password; //
    private String websiteUrl;
    private String notes;
    private @ServerTimestamp Date createdAt;
    private @ServerTimestamp Date updatedAt;
    private String userId; //

    public PasswordItem() {

    }

    public PasswordItem(String platformName, String username, String password, String websiteUrl, String notes, String userId) {
        this.platformName = platformName;
        this.username = username;
        this.password = password;
        this.websiteUrl = websiteUrl;
        this.notes = notes;
        this.userId = userId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
