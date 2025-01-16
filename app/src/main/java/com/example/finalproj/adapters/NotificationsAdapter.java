package com.example.finalproj.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproj.R;
import com.example.finalproj.model.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault());

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
    }

    public NotificationsAdapter(List<NotificationItem> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem notification = notifications.get(position);

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.time.setText(dateFormat.format(new Date(notification.getTimestamp())));

        // Set icon based on notification type
        int iconRes;
        switch (notification.getType()) {
            case PRICE_CHANGE:
                iconRes = R.drawable.ic_notifications;  // Using existing icon
                break;
            case TRANSACTION:
                iconRes = R.drawable.ic_trans;  // Using existing icon
                break;
            case PRICE_TARGET:
                iconRes = R.drawable.ic_watchlist;  // Using existing icon
                break;
            case WATCHLIST_UPDATE:
                iconRes = R.drawable.ic_watchlist;
                break;
            default:
                iconRes = R.drawable.ic_notifications;
        }
        holder.icon.setImageResource(iconRes);

        // Unread indicator
        holder.itemView.setAlpha(notification.isRead() ? 0.7f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView message;
        TextView time;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.notification_icon);
            title = view.findViewById(R.id.notification_title);
            message = view.findViewById(R.id.notification_message);
            time = view.findViewById(R.id.notification_time);
        }
    }
}