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

    public Job(String id, String title, String company, String description, String salary, String category,
               String employerDescription, float employerRating, String employerContact, String deadline) {
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
}
