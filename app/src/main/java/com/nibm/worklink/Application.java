package com.nibm.worklink;

public class Application {
    private String id;
    private Job job;
    private String coverLetter;
    private String resumeFileName;
    private String status;
    private String applicantName;
    private String applicantEmail;

    public Application(String id, Job job, String coverLetter, String resumeFileName, String status, String applicantName, String applicantEmail) {
        this.id = id;
        this.job = job;
        this.coverLetter = coverLetter;
        this.resumeFileName = resumeFileName;
        this.status = status;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
    }

    public String getId() { return id; }
    public Job getJob() { return job; }
    public String getCoverLetter() { return coverLetter; }
    public String getResumeFileName() { return resumeFileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getApplicantEmail() { return applicantEmail; }
    public void setApplicantEmail(String applicantEmail) { this.applicantEmail = applicantEmail; }

    private String freelancerUid;
    public String getFreelancerUid() { return freelancerUid; }
    public void setFreelancerUid(String freelancerUid) { this.freelancerUid = freelancerUid; }
}
