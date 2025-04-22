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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_CONTACT = 1;
    private static final int VIEW_TYPE_REQUEST = 2;

    private final Context context;
    private final List<ChatContact> contacts;
    private final OnChatContactListener listener;
    private boolean showingRequests = false;

    public interface OnChatContactListener {
        void onChatContactClick(ChatContact contact);
    }

    public ChatContactAdapter(Context context, List<ChatContact> contacts, OnChatContactListener listener) {
        this.context = context;
        this.contacts = contacts;
        this.listener = listener;
    }

    public void setShowingRequests(boolean showingRequests) {
        this.showingRequests = showingRequests;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return showingRequests ? VIEW_TYPE_REQUEST : VIEW_TYPE_CONTACT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_REQUEST) {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_chat_request, parent, false);
            return new RequestViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(
                    R.layout.item_chat_contact, parent, false);
            return new ContactViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatContact contact = contacts.get(position);

        if (holder instanceof ContactViewHolder) {
            bindContactViewHolder((ContactViewHolder) holder, contact);
        } else if (holder instanceof RequestViewHolder) {
            bindRequestViewHolder((RequestViewHolder) holder, contact);
        }
    }

    private void bindContactViewHolder(ContactViewHolder holder, ChatContact contact) {
        // Load profile photo
        if (contact.getPhotoUrl() != null && !contact.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(contact.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Set display name
        holder.displayName.setText(contact.getDisplayName());

        // Set last message and time
        if (contact.getLastMessage() != null && !contact.getLastMessage().isEmpty()) {
            holder.lastMessage.setText(contact.getLastMessage());

            // Format time
            if (contact.getLastMessageTime() > 0) {
                holder.lastMessageTime.setVisibility(View.VISIBLE);
                holder.lastMessageTime.setText(formatTime(contact.getLastMessageTime()));
            } else {
                holder.lastMessageTime.setVisibility(View.GONE);
            }
        } else {
            holder.lastMessage.setText("No messages yet");
            holder.lastMessageTime.setVisibility(View.GONE);
        }

        // Show unread count if any
        if (contact.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(contact.getUnreadCount()));
        } else {
            holder.unreadCount.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatContactClick(contact);
            }
        });
    }

    private void bindRequestViewHolder(RequestViewHolder holder, ChatContact contact) {
        // Load profile photo
        if (contact.getPhotoUrl() != null && !contact.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(contact.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Set display name and email
        holder.displayName.setText(contact.getDisplayName());
        holder.email.setText(contact.getEmail());

        // Set click listeners for buttons
        holder.acceptButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatContactClick(contact);
            }
        });
    }

    private String formatTime(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    // ViewHolder for regular contacts
    static class ContactViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView displayName;
        TextView lastMessage;
        TextView lastMessageTime;
        TextView unreadCount;

        ContactViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            displayName = itemView.findViewById(R.id.display_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            lastMessageTime = itemView.findViewById(R.id.last_message_time);
            unreadCount = itemView.findViewById(R.id.unread_count);
        }
    }

    // ViewHolder for connection requests
    static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView displayName;
        TextView email;
        Button acceptButton;

        RequestViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            displayName = itemView.findViewById(R.id.display_name);
            email = itemView.findViewById(R.id.email);
            acceptButton = itemView.findViewById(R.id.accept_button);
        }
    }
}