package com.aryan.edenic;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.ChatMessageAdapter;
import com.aryan.edenic.models.ChatMessage;
import com.aryan.edenic.models.Stock;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    // UI components
    private RecyclerView messagesList;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton backButton;
    private ImageView contactPhoto;
    private TextView contactName;
    private TextView emptyChat;

    // Data
    private List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private String currentUserId;
    private String contactId;
    private String contactNameStr;
    private String contactPhotoUrl;
    private String chatId; // Unique identifier for this conversation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        // Get contact details from intent
        contactId = getIntent().getStringExtra("contactId");
        contactNameStr = getIntent().getStringExtra("contactName");
        contactPhotoUrl = getIntent().getStringExtra("contactPhoto");

        if (contactId == null || contactNameStr == null) {
            Toast.makeText(this, "Contact information missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate chat ID (alphabetical order of user IDs)
        chatId = currentUserId.compareTo(contactId) < 0 ?
                currentUserId + "_" + contactId : contactId + "_" + currentUserId;

        // Initialize UI components
        initializeViews();
        setupListeners();
        loadContactPhoto();
        setupRecyclerView();

        // Load chat messages
        loadChatMessages();
    }

    private void initializeViews() {
        messagesList = findViewById(R.id.messages_list);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);
        contactPhoto = findViewById(R.id.contact_photo);
        contactName = findViewById(R.id.contact_name);
        emptyChat = findViewById(R.id.empty_chat);

        // Set contact name
        contactName.setText(contactNameStr);
    }

    private void loadContactPhoto() {
        if (contactPhotoUrl != null && !contactPhotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(contactPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.profile_placeholder)
                    .into(contactPhoto);
        }
    }

    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Enter key to send
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from the bottom
        messagesList.setLayoutManager(layoutManager);
        messagesList.setAdapter(adapter);

        // For smooth scrolling to bottom when keyboard appears
        messagesList.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && adapter.getItemCount() > 0) {
                messagesList.postDelayed(() ->
                        messagesList.smoothScrollToPosition(adapter.getItemCount() - 1), 100);
            }
        });
    }

    private void loadChatMessages() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(chatId);

        messagesRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messages.clear();

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    try {
                        ChatMessage chatMessage = messageSnapshot.getValue(ChatMessage.class);
                        if (chatMessage != null) {
                            messages.add(chatMessage);
                            Log.d(TAG, "Loaded message: " + chatMessage.getContent() +
                                    ", Time: " + chatMessage.getTimestamp() +
                                    ", Sender: " + (chatMessage.getSenderId().equals(currentUserId) ? "me" : "other"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing message", e);
                    }
                }

                // Sort messages by timestamp
                Collections.sort(messages, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                adapter.notifyDataSetChanged();

                if (!messages.isEmpty()) {
                    messagesList.scrollToPosition(messages.size() - 1);
                    emptyChat.setVisibility(View.GONE);
                } else {
                    emptyChat.setVisibility(View.VISIBLE);
                }

                // Mark messages as read
                markMessagesAsRead();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading messages", error.toException());
            }
        });
    }

    private void markMessagesAsRead() {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(chatId);

        for (ChatMessage message : messages) {
            if (!message.isRead() && !message.getSenderId().equals(currentUserId)) {
                messagesRef.child(message.getMessageId()).child("read").setValue(true);
            }
        }

        // Update unread count in contact
        updateUnreadCount(0);
    }

    private void updateUnreadCount(int count) {
        DatabaseReference contactRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("contacts")
                .child(contactId);

        contactRef.child("unreadCount").setValue(count);
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Create new message
        ChatMessage message = new ChatMessage(
                currentUserId,
                user.getDisplayName(),
                text
        );

        // Save to Firebase
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(chatId);

        messagesRef.child(message.getMessageId()).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Clear input
                    messageInput.setText("");

                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(messageInput.getWindowToken(), 0);

                    // Update last message and time for both users
                    updateLastMessage(message);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }

    public void sendStockShare(Stock stock) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Create stock share message
        ChatMessage message = new ChatMessage(
                currentUserId,
                user.getDisplayName(),
                stock.getSymbol(),
                stock.getPrice()
        );

        // Save to Firebase
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages")
                .child(chatId);

        messagesRef.child(message.getMessageId()).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Stock shared successfully", Toast.LENGTH_SHORT).show();

                    // Update last message and time for both users
                    updateLastMessage(message);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to share stock", Toast.LENGTH_SHORT).show());
    }

    private void updateLastMessage(ChatMessage message) {
        // Update in current user's contacts
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("contacts")
                .child(contactId);

        currentUserRef.child("lastMessage").setValue(message.getContent());
        currentUserRef.child("lastMessageTime").setValue(message.getTimestamp());

        // Update in contact's contacts
        DatabaseReference contactRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(contactId)
                .child("contacts")
                .child(currentUserId);

        contactRef.child("lastMessage").setValue(message.getContent());
        contactRef.child("lastMessageTime").setValue(message.getTimestamp());

        // Increment unread count for contact
        contactRef.child("unreadCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentCount = snapshot.getValue(Integer.class);
                int newCount = (currentCount != null ? currentCount : 0) + 1;
                contactRef.child("unreadCount").setValue(newCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}