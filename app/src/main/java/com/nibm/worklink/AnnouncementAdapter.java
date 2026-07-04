package com.nibm.worklink;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

    public interface OnAnnouncementActionListener {
        void onEditAnnouncement(String name, int position);
        void onDeleteAnnouncement(int position);
    }

    private final List<String> announcements;
    private final OnAnnouncementActionListener listener;

    public AnnouncementAdapter(List<String> announcements, OnAnnouncementActionListener listener) {
        this.announcements = announcements;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String ann = announcements.get(position);
        holder.tvName.setText(ann);
        holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);

        holder.btnEdit.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_ID) {
                listener.onEditAnnouncement(announcements.get(currentPos), currentPos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_ID) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Announcement")
                        .setMessage("Are you sure you want to delete '" + announcements.get(currentPos) + "'?")
                        .setPositiveButton("Delete", (d, w) -> {
                            listener.onDeleteAnnouncement(currentPos);
                            Toast.makeText(v.getContext(), "Deleted", Toast.LENGTH_SHORT).show();
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
        TextView tvName;
        ImageView ivIcon;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tv_category_name);
            ivIcon    = itemView.findViewById(R.id.iv_category_icon);
            btnEdit   = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
