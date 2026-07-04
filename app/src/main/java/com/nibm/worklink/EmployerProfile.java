package com.nibm.worklink;

public class EmployerProfile {
    private String companyName;
    private String email;
    private String contact;
    private String description;
    private float rating;

    public EmployerProfile(String companyName, String email, String contact, String description, float rating) {
        this.companyName = companyName;
        this.email = email;
        this.contact = contact;
        this.description = description;
        this.rating = rating;
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}
