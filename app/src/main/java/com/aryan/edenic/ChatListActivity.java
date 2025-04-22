package com.aryan.edenic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.ChatContactAdapter;
import com.aryan.edenic.models.ChatContact;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity implements ChatContactAdapter.OnChatContactListener {
    private static final String TAG = "ChatListActivity";

    // UI components
    private RecyclerView chatList;
    private LinearLayout emptyChats;
    private TextView emptyText;
    private Button findUsersEmpty;
    private ImageButton findUsersButton;
    private TabLayout chatTabs;
    private BottomNavigationView bottomNav;

    // Data
    private List<ChatContact> contacts = new ArrayList<>();
    private ChatContactAdapter adapter;
    private String currentUserId;
    private boolean showingRequests = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Check if user is signed in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Auth_Activity.class));
            finish();
            return;
        }

        currentUserId = user.getUid();

        // Initialize UI components
        initializeViews();
        setupListeners();
        setupRecyclerView();

        // Load chat contacts
        loadChatContacts();
    }

    private void initializeViews() {
        chatList = findViewById(R.id.chat_list);
        emptyChats = findViewById(R.id.empty_chats);
        emptyText = findViewById(R.id.empty_text);
        findUsersEmpty = findViewById(R.id.find_users_empty);
        findUsersButton = findViewById(R.id.find_users_button);
        chatTabs = findViewById(R.id.chat_tabs);
        bottomNav = findViewById(R.id.bottom_nav);
    }

    private void setupListeners() {
        // Find users buttons
        findUsersButton.setOnClickListener(v -> openUserSearch());
        findUsersEmpty.setOnClickListener(v -> openUserSearch());

        // Tab selection
        chatTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingRequests = tab.getPosition() == 1; // Second tab is requests
                loadChatContacts();

                if (showingRequests) {
                    emptyText.setText("No pending requests");
                } else {
                    emptyText.setText("No chats yet.\nConnect with other traders to start chatting!");
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Bottom navigation
        bottomNav.setSelectedItemId(R.id.nav_chat);
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_discovery) {
                startActivity(new Intent(this, Discover.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_portfolio) {
                startActivity(new Intent(this, PortfolioActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatContactAdapter(this, contacts, this);
        chatList.setLayoutManager(new LinearLayoutManager(this));
        chatList.setAdapter(adapter);
    }

    private void loadChatContacts() {
        contacts.clear();

        // Reference to appropriate node in Firebase
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child(showingRequests ? "requests" : "contacts");

        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contacts.clear();

                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    ChatContact contact = contactSnapshot.getValue(ChatContact.class);
                    if (contact != null) {
                        // For contacts, only show connected ones
                        if (!showingRequests && !contact.isConnected()) continue;

                        // For requests, only show pending ones
                        if (showingRequests && !contact.isPendingRequest()) continue;

                        contacts.add(contact);
                    }
                }

                adapter.notifyDataSetChanged();

                // Show/hide empty state
                boolean isEmpty = contacts.isEmpty();
                chatList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                emptyChats.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void openUserSearch() {
        startActivity(new Intent(this, UserSearchActivity.class));
    }

    @Override
    public void onChatContactClick(ChatContact contact) {
        if (showingRequests) {
            // Handle connection request
            acceptChatRequest(contact);
        } else {
            // Open chat
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("contactId", contact.getUserId());
            intent.putExtra("contactName", contact.getDisplayName());
            intent.putExtra("contactPhoto", contact.getPhotoUrl());
            startActivity(intent);
        }
    }

    private void acceptChatRequest(ChatContact contact) {
        // Update connection status in Firebase
        DatabaseReference contactRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("contacts")
                .child(contact.getUserId());

        contact.setConnected(true);
        contact.setPendingRequest(false);
        contactRef.setValue(contact);

        // Remove from requests
        FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("requests")
                .child(contact.getUserId())
                .removeValue();

        // Add current user to other user's contacts
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ChatContact currentUserContact = new ChatContact(
                    currentUser.getUid(),
                    currentUser.getDisplayName(),
                    currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null,
                    currentUser.getEmail()
            );

            currentUserContact.setConnected(true);

            FirebaseDatabase.getInstance().getReference("chats")
                    .child(contact.getUserId())
                    .child("contacts")
                    .child(currentUser.getUid())
                    .setValue(currentUserContact);
        }
    }
}