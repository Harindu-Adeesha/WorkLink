package com.nibm.worklink;

public class Announcement {
    private String id;
    private String title;
    private String message;
    private String targetAudience;
    private String priority;
    private String date;

    public Announcement() {
        // Default constructor required for Firebase Firestore
    }

    public Announcement(String id, String title, String message, String targetAudience, String priority, String date) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.targetAudience = targetAudience;
        this.priority = priority;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
