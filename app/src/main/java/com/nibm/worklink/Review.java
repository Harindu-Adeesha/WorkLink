package com.nibm.worklink;

public class Review {
    private String id;
    private String jobId;
    private float rating;
    private String reviewText;

    public Review(String id, String jobId, float rating, String reviewText) {
        this.id = id;
        this.jobId = jobId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public String getId() { return id; }
    public String getJobId() { return jobId; }
    public float getRating() { return rating; }
    public String getReviewText() { return reviewText; }
}
