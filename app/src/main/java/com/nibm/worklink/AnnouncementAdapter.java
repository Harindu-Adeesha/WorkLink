package com.nibm.worklink;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnAnnouncementActionListener {
        void onEditAnnouncement(Announcement announcement, int position);
        void onDeleteAnnouncement(Announcement announcement, int position);
    }

    private final List<Announcement> announcements;
    private final OnAnnouncementActionListener listener;

    public AnnouncementAdapter(List<Announcement> announcements, OnAnnouncementActionListener listener) {
        this.announcements = announcements;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Announcement ann = announcements.get(position);
        holder.tvTitle.setText(ann.getTitle() != null ? ann.getTitle() : "Announcement");
        holder.tvMessage.setText(ann.getMessage() != null ? ann.getMessage() : "");
        holder.tvDate.setText(ann.getDate() != null ? ann.getDate() : "");
        
        String audience = ann.getTargetAudience() != null ? ann.getTargetAudience() : "All Users";
        holder.tvAudience.setText(audience);

        String priority = ann.getPriority() != null ? ann.getPriority() : "Info";
        holder.tvPriority.setText(priority.toUpperCase());

        // Color coding by priority
        if ("Urgent".equalsIgnoreCase(priority)) {
            holder.tvPriority.setTextColor(Color.parseColor("#DC3545"));
            holder.tvPriority.setBackgroundColor(Color.parseColor("#F8D7DA"));
        } else if ("Important".equalsIgnoreCase(priority)) {
            holder.tvPriority.setTextColor(Color.parseColor("#FD7E14"));
            holder.tvPriority.setBackgroundColor(Color.parseColor("#FFE8CC"));
        } else if ("Maintenance".equalsIgnoreCase(priority)) {
            holder.tvPriority.setTextColor(Color.parseColor("#6C757D"));
            holder.tvPriority.setBackgroundColor(Color.parseColor("#E9ECEF"));
        } else {
            // Info / Default
            holder.tvPriority.setTextColor(Color.parseColor("#035DD6"));
            holder.tvPriority.setBackgroundColor(Color.parseColor("#E7F1FF"));
        }

        holder.btnEdit.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onEditAnnouncement(announcements.get(currentPos), currentPos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                Announcement itemToDelete = announcements.get(currentPos);
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Announcement")
                        .setMessage("Are you sure you want to delete '" + itemToDelete.getTitle() + "'?")
                        .setPositiveButton("Delete", (d, w) -> {
                            listener.onDeleteAnnouncement(itemToDelete, currentPos);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvPriority, tvAudience, tvDate;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_announcement_title);
            tvMessage  = itemView.findViewById(R.id.tv_announcement_message);
            tvPriority = itemView.findViewById(R.id.tv_announcement_priority);
            tvAudience = itemView.findViewById(R.id.tv_announcement_audience);
            tvDate     = itemView.findViewById(R.id.tv_announcement_date);
            btnEdit    = itemView.findViewById(R.id.btn_edit_announcement);
            btnDelete  = itemView.findViewById(R.id.btn_delete_announcement);
        }
    }
}

