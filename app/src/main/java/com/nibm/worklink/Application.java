package com.nibm.worklink;

public class Application {
    private String id;
    private Job job;
    private String coverLetter;
    private String resumeFileName;
    private String status; // "Pending", "Under Review", "Shortlisted", "Accepted", "Rejected"

    public Application(String id, Job job, String coverLetter, String resumeFileName, String status) {
        this.id = id;
        this.job = job;
        this.coverLetter = coverLetter;
        this.resumeFileName = resumeFileName;
        this.status = status;
    }

    public String getId() { return id; }
    public Job getJob() { return job; }
    public String getCoverLetter() { return coverLetter; }
    public String getResumeFileName() { return resumeFileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
