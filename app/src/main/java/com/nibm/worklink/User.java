package com.nibm.worklink;

public class User {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String skills;
    private String bio;
    private boolean isVerified = false;

    public User() {
        // Default constructor required for Firebase Firestore
    }

    public User(String uid, String name, String email, String role, String skills, String bio) {
        this(uid, name, email, role, skills, bio, false);
    }

    public User(String uid, String name, String email, String role, String skills, String bio, boolean isVerified) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.skills = skills;
        this.bio = bio;
        this.isVerified = isVerified;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getDetails() {
        if (skills != null && !skills.trim().isEmpty()) {
            return skills;
        }
        if (bio != null && !bio.trim().isEmpty()) {
            return bio;
        }
        return "No bio/skills specified";
    }
}
