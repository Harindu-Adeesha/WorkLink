package com.nibm.worklink;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Fetches all job reviews from Firestore and computes average rating per jobId.
 */
public class RatingRepository {

    public interface RatingsCallback {
        /** Called with a map of jobId -> averageRating (may be empty, never null). */
        void onRatingsLoaded(Map<String, Float> ratingsMap);
    }

    /**
     * Asynchronously fetches all reviews and computes per-job average ratings.
     * @param callback invoked on success or failure (empty map on failure)
     */
    public static void fetchJobRatings(RatingsCallback callback) {
        FirebaseFirestore.getInstance()
            .collection("Reviews")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                // sum & count per jobId
                Map<String, Float> sumMap   = new HashMap<>();
                Map<String, Integer> countMap = new HashMap<>();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String jobId = doc.getString("jobId");
                    Double ratingVal = doc.getDouble("rating");
                    if (jobId == null || jobId.isEmpty() || ratingVal == null) continue;

                    float r = ratingVal.floatValue();
                    sumMap.put(jobId, sumMap.getOrDefault(jobId, 0f) + r);
                    countMap.put(jobId, countMap.getOrDefault(jobId, 0) + 1);
                }

                Map<String, Float> avgMap = new HashMap<>();
                for (String jobId : sumMap.keySet()) {
                    float sum   = sumMap.get(jobId);
                    int   count = countMap.get(jobId);
                    // round to 1 decimal
                    float avg = Math.round((sum / count) * 10f) / 10f;
                    avgMap.put(jobId, avg);
                }

                callback.onRatingsLoaded(avgMap);
            })
            .addOnFailureListener(e -> callback.onRatingsLoaded(new HashMap<>()));
    }
}
