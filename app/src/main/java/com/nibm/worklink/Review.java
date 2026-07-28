package com.nibm.worklink;

public class Review {
    private String id;
    private String jobId;
    private String jobTitle;
    private String company;
    private String reviewerUid;
    private float rating;
    private String reviewText;
    private long createdAt;

    // Required by Firestore
    public Review() {}

    public Review(String id, String jobId, float rating, String reviewText) {
        this.id = id;
        this.jobId = jobId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getReviewerUid() { return reviewerUid; }
    public void setReviewerUid(String reviewerUid) { this.reviewerUid = reviewerUid; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
