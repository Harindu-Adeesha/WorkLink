package com.nibm.worklink;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudinaryUploader {

    public static final String CLOUD_NAME = "c2ijdvdp";
    public static final String API_KEY    = "983194518433898";
    public static final String API_SECRET = "jisnbzA3sfvW0r6rCPYwUYKgHP8";

    public interface UploadCallback {
        void onSuccess(String url);
        void onError(String error);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void uploadFile(Context context, Uri uri, String cloudName, String uploadPreset, UploadCallback callback) {
        executor.execute(() -> {
            try {
                String targetCloudName = (cloudName != null && !cloudName.trim().isEmpty()) ? cloudName : CLOUD_NAME;

                URL url = new URL("https://api.cloudinary.com/v1_1/" + targetCloudName + "/auto/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");

                String boundary = "*****" + UUID.randomUUID().toString() + "*****";
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();

                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String stringToSign = "timestamp=" + timestamp + API_SECRET;
                String signature = getSha1Hex(stringToSign);

                // Write signed credentials
                writeFormField(outputStream, boundary, "api_key", API_KEY);
                writeFormField(outputStream, boundary, "timestamp", timestamp);
                writeFormField(outputStream, boundary, "signature", signature);

                if (uploadPreset != null && !uploadPreset.trim().isEmpty()) {
                    writeFormField(outputStream, boundary, "upload_preset", uploadPreset);
                }

                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("Unable to open selected CV file");
                }

                String fileName = getFileName(context, uri);
                writeFileField(outputStream, boundary, "file", fileName, inputStream);

                byte[] endBoundary = ("--" + boundary + "--\r\n").getBytes("UTF-8");
                outputStream.write(endBoundary);
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = responseStream.read(buffer)) != -1) {
                    resultStream.write(buffer, 0, length);
                }
                responseStream.close();
                String responseString = resultStream.toString("UTF-8");

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(responseString);
                    String secureUrl = json.optString("secure_url", json.optString("url", ""));
                    if (!secureUrl.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(secureUrl));
                    } else {
                        mainHandler.post(() -> callback.onError("Cloudinary response missing secure_url"));
                    }
                } else if (responseCode == 401) {
                    mainHandler.post(() -> callback.onError("HTTP 401 Unauthorized: Credentials for Cloud Name '" + targetCloudName + "' were rejected. Response: " + responseString));
                } else {
                    mainHandler.post(() -> callback.onError("Upload failed (Code " + responseCode + "): " + responseString));
                }

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Upload error: " + e.getMessage()));
            }
        });
    }

    private static String getSha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void writeFormField(OutputStream out, String boundary, String name, String value) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        sb.append(value).append("\r\n");
        out.write(sb.toString().getBytes("UTF-8"));
    }

    private static void writeFileField(OutputStream out, String boundary, String fieldName, String fileName, InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(fieldName).append("\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        out.write(sb.toString().getBytes("UTF-8"));

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        out.write("\r\n".getBytes("UTF-8"));
    }

    private static String getFileName(Context context, Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
            return path;
        }
        return "cv_resume.pdf";
    }
}
