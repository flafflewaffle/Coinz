package com.example.s1623165.coinz;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>{

    private ArrayList<Notification> notifications;

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        public ImageView noteImage;
        public TextView title;
        public TextView description;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            noteImage = itemView.findViewById(R.id.noteImage);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);

        }
    }

    public NotificationsAdapter(ArrayList<Notification> noteList) {
        notifications = noteList;
    }

    @NonNull
    @Override
    public NotificationsAdapter.NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.notification_item, viewGroup, false);
        NotificationsAdapter.NotificationViewHolder  nvh = new NotificationsAdapter.NotificationViewHolder(v);
        return nvh;
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationsAdapter.NotificationViewHolder notificationViewHolder, int i) {
        Notification note = notifications.get(i);

        notificationViewHolder.noteImage.setImageResource(note.getNoteImageResource());
        notificationViewHolder.title.setText(note.getTitle());
        notificationViewHolder.description.setText(note.getDescription());
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}
