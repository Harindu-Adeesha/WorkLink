package com.nibm.worklink;

import java.util.ArrayList;
import java.util.List;

public class DataManager {



    // Static Lists & Objects (In-Memory Database)
    private static List<Job> jobs = new ArrayList<>();
    private static List<Application> applications = new ArrayList<>();
    private static List<Review> reviews = new ArrayList<>();
    private static List<Notification> notifications = new ArrayList<>();
    private static Profile freelancerProfile = null;
    private static EmployerProfile employerProfile = null;

    static {
        // Prepopulate Job Feed
        jobs.add(new Job("1", "Senior Android Developer", "Google LLC",
                "We are looking for a Senior Android Developer to design, build, and maintain our next-generation mobile applications.",
                "$120k - $150k / year", "Software Development",
                "Google LLC is a global technology leader focused on improving the ways people connect with information.",
                4.8f, "careers@google.com", "2026-07-31"));

        jobs.add(new Job("2", "UI/UX Designer", "Figma Inc",
                "Join our design team to craft beautiful interfaces and optimize user journeys for millions of designers worldwide.",
                "$90k - $110k / year", "UI/UX Design",
                "Figma is a collaborative web application for interface design, with additional offline features enabled by desktop applications.",
                4.7f, "design@figma.com", "2026-08-15"));

        jobs.add(new Job("3", "Technical Content Writer", "Medium",
                "Write clear, concise technical articles and documentation explaining complex software engineering concepts.",
                "$45 - $60 / hour", "Content Writing",
                "Medium is an open platform where over 100 million readers come to find insightful and dynamic thinking.",
                4.3f, "editor@medium.com", "2026-07-20"));

        jobs.add(new Job("4", "Growth Marketing Manager", "Netflix",
                "Drive subscriber acquisition and design performance marketing campaigns across channels for digital video streaming.",
                "$100k - $130k / year", "Digital Marketing",
                "Netflix, Inc. is an American media-services provider and production company headquartered in Los Gatos, California.",
                4.6f, "recruiting@netflix.com", "2026-08-01"));

        jobs.add(new Job("5", "Mobile Developer (Kotlin)", "Spotify",
                "Develop outstanding user experiences on Spotify's Android app using modern tools, libraries, and Kotlin programming.",
                "$110k - $135k / year", "Software Development",
                "Spotify is a digital music, podcast, and video service that gives you access to millions of songs.",
                4.9f, "jobs@spotify.com", "2026-07-25"));

        // Hardcoded Reviews (simulating reviews submitted by Freelancers)
        reviews.add(new Review("1", "1", 5.0f, "Google was an amazing client! Project scope was crystal clear and payments were prompt. Highly recommend."));
        reviews.add(new Review("2", "1", 4.5f, "Excellent communication throughout the project. The team at Google was very responsive and professional."));
        reviews.add(new Review("3", "2", 4.0f, "Figma had a well-structured brief. Minor changes were requested midway but overall a great experience."));
        reviews.add(new Review("4", "2", 3.5f, "Good opportunity but requirements shifted a bit during the project. Communication could have been better."));
        reviews.add(new Review("5", "3", 5.0f, "Medium's editorial team was fantastic to work with. Clear guidelines and very supportive feedback."));
        reviews.add(new Review("6", "4", 4.8f, "Netflix provided a very professional environment. Well-defined goals and the pay was on time."));
        
        // Hardcoded Applications
        applications.add(new Application("1", jobs.get(0), "I have 5 years of Android experience. My email is john.doe@example.com.", "john_doe_resume.pdf", "Pending"));
        applications.add(new Application("2", jobs.get(1), "UI/UX designer with a passion for clean interfaces. Contact: jane.smith@example.com", "jane_smith_resume.pdf", "Accepted"));
        applications.add(new Application("3", jobs.get(0), "Kotlin expert here. Email: mike.j@example.com", "mike_j_resume.pdf", "Rejected"));
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

    public static void addJob(Job job) {
        jobs.add(job);
    }

    public static boolean deleteJob(String id) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId().equals(id)) {
                jobs.remove(i);
                // Also remove any applications for this job
                for (int j = applications.size() - 1; j >= 0; j--) {
                    if (applications.get(j).getJob().getId().equals(id)) {
                        applications.remove(j);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static boolean updateJob(Job updatedJob) {
        for (int i = 0; i < jobs.size(); i++) {
            if (jobs.get(i).getId().equals(updatedJob.getId())) {
                jobs.set(i, updatedJob);
                return true;
            }
        }
        return false;
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

    public static EmployerProfile getEmployerProfile() {
        return employerProfile;
    }

    public static void setEmployerProfile(EmployerProfile profile) {
        employerProfile = profile;
    }

    public static void deleteEmployerProfile() {
        employerProfile = null;
    }

    public static List<Review> getReviews() {
        return reviews;
    }

    public static void addReview(Review review) {
        reviews.add(review);
    }

    public static List<Notification> getNotifications() {
        return notifications;
    }

    public static void addNotification(Notification notification) {
        notifications.add(notification);
    }
}
