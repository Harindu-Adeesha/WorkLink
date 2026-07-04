package com.nibm.worklink;

public class Profile {
    private String name;
    private String email;
    private String title;
    private String bio;
    private String skills;
    private String hourlyRate;

    public Profile(String name, String email, String title, String bio, String skills, String hourlyRate) {
        this.name = name;
        this.email = email;
        this.title = title;
        this.bio = bio;
        this.skills = skills;
        this.hourlyRate = hourlyRate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(String hourlyRate) { this.hourlyRate = hourlyRate; }
}
