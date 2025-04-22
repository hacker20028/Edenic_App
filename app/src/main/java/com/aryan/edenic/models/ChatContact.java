package com.aryan.edenic.models;

public class ChatContact {
    private String userId;
    private String displayName;
    private String photoUrl;
    private String email;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean isConnected; // Whether user has accepted connection
    private boolean pendingRequest; // Whether there's a pending request

    public ChatContact() {
        // Required empty constructor for Firebase
    }

    public ChatContact(String userId, String displayName, String photoUrl, String email) {
        this.userId = userId;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.email = email;
        this.lastMessage = "";
        this.lastMessageTime = 0;
        this.unreadCount = 0;
        this.isConnected = false;
        this.pendingRequest = false;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { isConnected = connected; }

    public boolean isPendingRequest() { return pendingRequest; }
    public void setPendingRequest(boolean pendingRequest) { this.pendingRequest = pendingRequest; }
}