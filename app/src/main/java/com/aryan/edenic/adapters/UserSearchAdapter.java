package com.aryan.edenic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.R;
import com.aryan.edenic.models.ChatContact;
import com.bumptech.glide.Glide;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
    private final Context context;
    private final List<ChatContact> users;
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onSendRequest(ChatContact contact);
    }

    public UserSearchAdapter(Context context, List<ChatContact> users, OnUserActionListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.item_user_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatContact user = users.get(position);

        // Load profile image
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Set user info
        holder.displayName.setText(user.getDisplayName());
        holder.email.setText(user.getEmail());

        // Configure button state based on connection status
        if (user.isConnected()) {
            // Already connected
            holder.actionButton.setText("CONNECTED");
            holder.actionButton.setEnabled(false);
            holder.actionButton.setBackgroundResource(R.drawable.button_disabled_background);
        } else if (user.isPendingRequest()) {
            // Request pending
            holder.actionButton.setText("REQUEST SENT");
            holder.actionButton.setEnabled(false);
            holder.actionButton.setBackgroundResource(R.drawable.button_disabled_background);
        } else {
            // Can send request
            holder.actionButton.setText("CONNECT");
            holder.actionButton.setEnabled(true);
            holder.actionButton.setBackgroundResource(R.drawable.button_primary_background);

            holder.actionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSendRequest(user);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView displayName;
        TextView email;
        Button actionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            displayName = itemView.findViewById(R.id.display_name);
            email = itemView.findViewById(R.id.email);
            actionButton = itemView.findViewById(R.id.action_button);
        }
    }
}