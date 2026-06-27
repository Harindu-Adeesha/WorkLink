package com.nibm.worklink;

import java.util.ArrayList;
import java.util.List;

public class DataManager {

    // Model Classes
    public static class Job {
        private String id;
        private String title;
        private String company;
        private String description;
        private String salary;
        private String category;
        private String employerDescription;
        private float employerRating;
        private String employerContact;

        public Job(String id, String title, String company, String description, String salary, String category,
                   String employerDescription, float employerRating, String employerContact) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.description = description;
            this.salary = salary;
            this.category = category;
            this.employerDescription = employerDescription;
            this.employerRating = employerRating;
            this.employerContact = employerContact;
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
    }

    public static class Application {
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

    public static class Profile {
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

    public static class Review {
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

    // Static Lists & Objects (In-Memory Database)
    private static List<Job> jobs = new ArrayList<>();
    private static List<Application> applications = new ArrayList<>();
    private static List<Review> reviews = new ArrayList<>();
    private static Profile freelancerProfile = null;

    static {
        // Prepopulate Job Feed
        jobs.add(new Job("1", "Senior Android Developer", "Google LLC",
                "We are looking for a Senior Android Developer to design, build, and maintain our next-generation mobile applications.",
                "$120k - $150k / year", "Software Development",
                "Google LLC is a global technology leader focused on improving the ways people connect with information.",
                4.8f, "careers@google.com"));

        jobs.add(new Job("2", "UI/UX Designer", "Figma Inc",
                "Join our design team to craft beautiful interfaces and optimize user journeys for millions of designers worldwide.",
                "$90k - $110k / year", "UI/UX Design",
                "Figma is a collaborative web application for interface design, with additional offline features enabled by desktop applications.",
                4.7f, "design@figma.com"));

        jobs.add(new Job("3", "Technical Content Writer", "Medium",
                "Write clear, concise technical articles and documentation explaining complex software engineering concepts.",
                "$45 - $60 / hour", "Content Writing",
                "Medium is an open platform where over 100 million readers come to find insightful and dynamic thinking.",
                4.3f, "editor@medium.com"));

        jobs.add(new Job("4", "Growth Marketing Manager", "Netflix",
                "Drive subscriber acquisition and design performance marketing campaigns across channels for digital video streaming.",
                "$100k - $130k / year", "Digital Marketing",
                "Netflix, Inc. is an American media-services provider and production company headquartered in Los Gatos, California.",
                4.6f, "recruiting@netflix.com"));

        jobs.add(new Job("5", "Mobile Developer (Kotlin)", "Spotify",
                "Develop outstanding user experiences on Spotify's Android app using modern tools, libraries, and Kotlin programming.",
                "$110k - $135k / year", "Software Development",
                "Spotify is a digital music, podcast, and video service that gives you access to millions of songs.",
                4.9f, "jobs@spotify.com"));
    }

    // Accessors
    public static List<Job> getJobs() {
        return jobs;
    }

    public static List<Job> getJobsByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return jobs;
        }
        List<Job> filtered = new ArrayList<>();
        for (Job job : jobs) {
            if (job.getCategory().equalsIgnoreCase(category)) {
                filtered.add(job);
            }
        }
        return filtered;
    }

    public static Job getJobById(String id) {
        for (Job job : jobs) {
            if (job.getId().equals(id)) {
                return job;
            }
        }
        return null;
    }

    public static List<Application> getApplications() {
        return applications;
    }

    public static void addApplication(Application application) {
        applications.add(application);
    }

    public static boolean deleteApplication(String id) {
        for (int i = 0; i < applications.size(); i++) {
            if (applications.get(i).getId().equals(id)) {
                applications.remove(i);
                return true;
            }
        }
        return false;
    }

    public static Profile getProfile() {
        return freelancerProfile;
    }

    public static void setProfile(Profile profile) {
        freelancerProfile = profile;
    }

    public static void deleteProfile() {
        freelancerProfile = null;
    }

    public static List<Review> getReviews() {
        return reviews;
    }

    public static void addReview(Review review) {
        reviews.add(review);
    }
}
