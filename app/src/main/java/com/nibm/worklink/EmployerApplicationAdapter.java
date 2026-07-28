package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

public class EmployerApplicationAdapter extends RecyclerView.Adapter<EmployerApplicationAdapter.ViewHolder> {

    public interface OnApplicationStatusListener {
        void onGiveStatus(Application app);
        void onDismissApplication(String appId);
    }

    private List<Application> appsList;
    private final OnApplicationStatusListener listener;

    public EmployerApplicationAdapter(List<Application> appsList, OnApplicationStatusListener listener) {
        this.appsList = appsList;
        this.listener = listener;
    }

    public void updateApplications(List<Application> newList) {
        this.appsList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application app = appsList.get(position);
        holder.tvTitle.setText(app.getJob().getTitle());
        
        String cv = app.getResumeFileName();
        if (cv != null && (cv.startsWith("http://") || cv.startsWith("https://"))) {
            holder.btnDownloadCv.setVisibility(View.VISIBLE);
            holder.btnDownloadCv.setOnClickListener(v -> {
                Context ctx = v.getContext();
                String rawName = app.getApplicantName() != null ? app.getApplicantName() : "Applicant";
                String cleanName = rawName.replaceAll("[^a-zA-Z0-9._-]", "_");
                String fileName = "CV_" + cleanName + ".pdf";

                new AlertDialog.Builder(ctx)
                    .setTitle("Applicant CV / Resume")
                    .setMessage("Choose how you would like to open " + rawName + "'s CV:")
                    .setPositiveButton("Download & Open", (dialog, which) -> {
                        downloadAndOpenCv(ctx, cv, fileName);
                    })
                    .setNeutralButton("View Online", (dialog, which) -> {
                        openInBrowser(ctx, cv);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        } else {
            holder.btnDownloadCv.setVisibility(View.GONE);
            holder.btnDownloadCv.setOnClickListener(null);
        }
        holder.tvCover.setText(app.getCoverLetter());
        holder.tvStatus.setText(app.getStatus());

        // Set dynamic applicant info
        String aName = app.getApplicantName() != null ? app.getApplicantName() : "Applicant";
        String aEmail = app.getApplicantEmail() != null ? app.getApplicantEmail() : "No Email";
        holder.tvApplicant.setText("Applicant: " + aName + " (" + aEmail + ")");

        // Set status tag styling
        if ("Accepted".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFF198754);
            holder.tvStatus.setBackgroundColor(0xFFD1E7DD);
        } else if ("Rejected".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFFDC3545);
            holder.tvStatus.setBackgroundColor(0xFFF8D7DA);
        } else {
            holder.tvStatus.setTextColor(0xFF856404);
            holder.tvStatus.setBackgroundColor(0xFFFFF3CD);
        }

        holder.btnGiveStatus.setOnClickListener(v -> listener.onGiveStatus(app));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Dismiss Application")
                    .setMessage("Are you sure you want to dismiss this application from the feed?")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        listener.onDismissApplication(app.getId());
                        Toast.makeText(v.getContext(), "Application dismissed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvApplicant, tvCover, tvStatus;
        Button btnGiveStatus, btnDelete, btnDownloadCv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tv_employer_app_title);
            tvApplicant   = itemView.findViewById(R.id.tv_employer_app_applicant);
            btnDownloadCv = itemView.findViewById(R.id.btn_download_cv);
            tvCover       = itemView.findViewById(R.id.tv_employer_app_cover);
            tvStatus      = itemView.findViewById(R.id.tv_employer_app_status);
            btnGiveStatus = itemView.findViewById(R.id.btn_employer_app_give_status);
            btnDelete     = itemView.findViewById(R.id.btn_employer_app_delete);
        }
    }



    private void downloadAndOpenCv(Context ctx, String cvUrl, String fileName) {
        Toast.makeText(ctx, "Downloading CV...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> urlsToTry = new java.util.ArrayList<>();
            urlsToTry.add(cvUrl);
            if (cvUrl.contains("cloudinary.com")) {
                if (!cvUrl.contains("/fl_attachment/")) {
                    urlsToTry.add(cvUrl.replace("/upload/", "/upload/fl_attachment/"));
                }
                if (cvUrl.toLowerCase().endsWith(".pdf")) {
                    urlsToTry.add(cvUrl.replaceAll("(?i)\\.pdf$", ".jpg"));
                }
            }

            HttpURLConnection conn = null;
            String successfulUrl = null;
            InputStream inputStream = null;

            for (String targetUrl : urlsToTry) {
                try {
                    URL url = new URL(targetUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        successfulUrl = targetUrl;
                        inputStream = conn.getInputStream();
                        break;
                    } else {
                        conn.disconnect();
                    }
                } catch (Exception ignored) {}
            }

            if (inputStream != null && successfulUrl != null) {
                try {
                    String finalFileName = successfulUrl.endsWith(".jpg") ? fileName.replace(".pdf", ".jpg") : fileName;
                    File downloadsDir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (downloadsDir != null && !downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }
                    File outputFile = new File(downloadsDir, finalFileName);

                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                    final String mimeType = finalFileName.endsWith(".jpg") ? "image/jpeg" : "application/pdf";
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ctx, "CV downloaded!", Toast.LENGTH_SHORT).show();
                        openDownloadedFile(ctx, outputFile, mimeType);
                    });
                    return;
                } catch (Exception ignored) {}
            }

            final String viewerUrl = "https://docs.google.com/viewer?url=" + Uri.encode(cvUrl);
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(ctx, "Opening in viewer...", Toast.LENGTH_SHORT).show();
                openInBrowser(ctx, viewerUrl);
            });
        });
    }

    private void openDownloadedFile(Context context, File file, String mimeType) {
        try {
            Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, "Open CV with..."));
        } catch (Exception e) {
            Toast.makeText(context, "Downloaded to app storage. No viewer app found.", Toast.LENGTH_LONG).show();
        }
    }

    private void openInBrowser(Context context, String url) {
        try {
            String targetUrl = url;
            if (url.toLowerCase().endsWith(".pdf") && !url.contains("docs.google.com")) {
                targetUrl = "https://docs.google.com/viewer?url=" + Uri.encode(url);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
}
