package com.nibm.worklink;

public class Job {
    private String id;
    private String title;
    private String company;
    private String description;
    private String salary;
    private String category;
    private String employerDescription;
    private float employerRating;
    private String employerContact;
    private String deadline;
    private String status = "Pending";
    private boolean isVerified = false;

    public Job() {}

    public Job(String id, String title, String company, String description, String salary, String category,
               String employerDescription, float employerRating, String employerContact, String deadline) {
        this(id, title, company, description, salary, category, employerDescription, employerRating, employerContact, deadline, "Pending", false);
    }

    public Job(String id, String title, String company, String description, String salary, String category,
               String employerDescription, float employerRating, String employerContact, String deadline,
               String status, boolean isVerified) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.description = description;
        this.salary = salary;
        this.category = category;
        this.employerDescription = employerDescription;
        this.employerRating = employerRating;
        this.employerContact = employerContact;
        this.deadline = deadline;
        this.status = status != null ? status : "Pending";
        this.isVerified = isVerified;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getDescription() { return description; }
    public String getSalary() { return salary; }
    public String getCategory() { return category; }
    public String getEmployerDescription() { return employerDescription; }
    public float getEmployerRating() { return employerRating; }
    public String getEmployerContact() { return employerContact; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getStatus() { return status != null ? status : "Pending"; }
    public void setStatus(String status) { this.status = status; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
}
