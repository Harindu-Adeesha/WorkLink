package com.nibm.worklink;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class EmployerNotificationAdapter extends RecyclerView.Adapter<EmployerNotificationAdapter.ViewHolder> {

    // Wrapper that holds either a Notification or an Announcement, plus a sortable timestamp
    public static class NotificationItem {
        public static final int TYPE_NOTIFICATION = 0;
        public static final int TYPE_ANNOUNCEMENT = 1;

        public final int type;
        public final Notification notification;
        public final Announcement announcement;
        public final long sortTimestamp; // millis, used for ordering both types

        public NotificationItem(Notification notification) {
            this.type = TYPE_NOTIFICATION;
            this.notification = notification;
            this.announcement = null;
            this.sortTimestamp = notification != null ? notification.getTimestamp() : 0;
        }

        public NotificationItem(Announcement announcement, long createdAtMillis) {
            this.type = TYPE_ANNOUNCEMENT;
            this.announcement = announcement;
            this.notification = null;
            this.sortTimestamp = createdAtMillis;
        }

        // Keep backwards-compat helper used in NotificationsActivity sort
        public long getTimestamp() {
            return sortTimestamp;
        }

        public String getTitle() {
            return type == TYPE_NOTIFICATION
                    ? (notification != null ? notification.getTitle() : "")
                    : (announcement != null ? announcement.getTitle() : "");
        }

        public String getMessage() {
            return type == TYPE_NOTIFICATION
                    ? (notification != null ? notification.getMessage() : "")
                    : (announcement != null ? announcement.getMessage() : "");
        }
    }

    private final List<NotificationItem> items;

    public EmployerNotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_mixed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvDate.setText(formatTimeDiff(item.sortTimestamp));

        if (item.type == NotificationItem.TYPE_NOTIFICATION) {
            // --- NOTIFICATION card ---
            holder.tvTypeBadge.setText("NOTIFICATION");
            holder.tvTypeBadge.setBackgroundResource(R.drawable.badge_type_bg);
            holder.ivIcon.setImageResource(android.R.drawable.ic_popup_reminder);
            // Tint icon blue
            holder.ivIcon.setColorFilter(Color.parseColor("#035DD6"));

            // Hide announcement-only section
            holder.llAnnouncementAttrs.setVisibility(View.GONE);

        } else {
            // --- ANNOUNCEMENT card ---
            holder.tvTypeBadge.setText("ANNOUNCEMENT");
            holder.tvTypeBadge.setBackgroundColor(Color.parseColor("#E67E22")); // orange for announcements
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_send);
            holder.ivIcon.setColorFilter(Color.parseColor("#E67E22"));

            // Show announcement attributes
            holder.llAnnouncementAttrs.setVisibility(View.VISIBLE);

            if (item.announcement != null) {
                String priority = item.announcement.getPriority();
                String audience = item.announcement.getTargetAudience();

                holder.tvAnnPriority.setText(priority != null ? priority.toUpperCase(Locale.getDefault()) : "");
                holder.tvAnnAudience.setText("For: " + (audience != null ? audience : "All"));

                // Color priority badge by level
                if ("High".equalsIgnoreCase(priority)) {
                    holder.tvAnnPriority.setBackgroundResource(R.drawable.badge_priority_bg); // red
                } else if ("Medium".equalsIgnoreCase(priority)) {
                    holder.tvAnnPriority.setBackgroundColor(Color.parseColor("#FFC107")); // amber
                    holder.tvAnnPriority.setTextColor(Color.parseColor("#14213d"));
                } else {
                    holder.tvAnnPriority.setBackgroundColor(Color.parseColor("#198754")); // green
                    holder.tvAnnPriority.setTextColor(Color.WHITE);
                }
            }
        }
    }

    private String formatTimeDiff(long timestampMs) {
        if (timestampMs <= 0) return "";
        long diff = System.currentTimeMillis() - timestampMs;
        long minutes = diff / (60 * 1000);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hr ago";
        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTypeBadge, tvTitle, tvMessage, tvDate;
        LinearLayout llAnnouncementAttrs;
        TextView tvAnnPriority, tvAnnAudience;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon               = itemView.findViewById(R.id.iv_notification_icon);
            tvTypeBadge          = itemView.findViewById(R.id.tv_type_badge);
            tvTitle              = itemView.findViewById(R.id.tv_notification_title);
            tvMessage            = itemView.findViewById(R.id.tv_notification_message);
            tvDate               = itemView.findViewById(R.id.tv_notification_date);
            llAnnouncementAttrs  = itemView.findViewById(R.id.ll_announcement_attrs);
            tvAnnPriority        = itemView.findViewById(R.id.tv_ann_priority);
            tvAnnAudience        = itemView.findViewById(R.id.tv_ann_audience);
        }
    }
}
