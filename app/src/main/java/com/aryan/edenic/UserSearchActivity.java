package com.aryan.edenic;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aryan.edenic.adapters.UserSearchAdapter;
import com.aryan.edenic.models.ChatContact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserSearchActivity extends AppCompatActivity implements UserSearchAdapter.OnUserActionListener {
    private static final String TAG = "UserSearchActivity";

    // UI components
    private EditText searchInput;
    private RecyclerView userList;
    private TextView emptyResults;
    private ImageButton backButton;
    private ImageButton searchButton;
    private ProgressBar searchProgress;

    // Data
    private List<ChatContact> users = new ArrayList<>();
    private UserSearchAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_search);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        currentUserId = currentUser.getUid();
        Log.d(TAG, "Current user ID: " + currentUserId);

        // Initialize UI components
        initializeViews();
        setupListeners();
        setupRecyclerView();
    }

    private void initializeViews() {
        searchInput = findViewById(R.id.search_input);
        userList = findViewById(R.id.user_list);
        emptyResults = findViewById(R.id.empty_results);
        backButton = findViewById(R.id.back_button);
        searchButton = findViewById(R.id.search_button);
        searchProgress = findViewById(R.id.search_progress);

        // Set initial empty results message
        emptyResults.setText("Search for other Edenic users\nby name or email");
        emptyResults.setVisibility(View.VISIBLE);
    }

    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Search button
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                String query = searchInput.getText().toString().trim();
                if (query.length() >= 3) {
                    searchUsers(query);
                } else {
                    Toast.makeText(this, "Enter at least 3 characters", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 3) {
                    // Add a slight delay to avoid too many rapid searches
                    searchInput.removeCallbacks(searchRunnable);
                    searchInput.postDelayed(searchRunnable, 500); // 500ms delay
                } else {
                    users.clear();
                    adapter.notifyDataSetChanged();
                    emptyResults.setText("Enter at least 3 characters to search");
                    emptyResults.setVisibility(View.VISIBLE);
                    searchProgress.setVisibility(View.GONE);
                }
            }
        });

        // Handle search action from keyboard
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (query.length() >= 3) {
                    searchUsers(query);
                } else {
                    Toast.makeText(this, "Enter at least 3 characters", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private final Runnable searchRunnable = new Runnable() {
        @Override
        public void run() {
            searchUsers(searchInput.getText().toString().trim());
        }
    };

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter(this, users, this);
        userList.setLayoutManager(new LinearLayoutManager(this));
        userList.setAdapter(adapter);
    }

    private void searchUsers(String query) {
        // Show progress and hide empty view
        searchProgress.setVisibility(View.VISIBLE);
        emptyResults.setVisibility(View.GONE);

        users.clear();
        adapter.notifyDataSetChanged();

        Log.d(TAG, "Searching for users with query: " + query);

        // First, check if we have any users at all in the database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.limitToFirst(5).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Found " + snapshot.getChildrenCount() + " users in database");

                if (snapshot.getChildrenCount() == 0) {
                    // No users in database
                    searchProgress.setVisibility(View.GONE);
                    emptyResults.setText("No users found in database");
                    emptyResults.setVisibility(View.VISIBLE);
                    return;
                }

                // Now do the actual search
                performUserSearch(query);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking users", error.toException());
                searchProgress.setVisibility(View.GONE);
                emptyResults.setText("Error connecting to database");
                emptyResults.setVisibility(View.VISIBLE);
            }
        });
    }

    private void performUserSearch(String query) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        Log.d(TAG, "Performing search with query: " + query);

        // Search by display name
        usersRef.orderByChild("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Name search found " + snapshot.getChildrenCount() + " results");

                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            // Skip current user
                            if (userSnapshot.getKey().equals(currentUserId)) {
                                Log.d(TAG, "Skipping current user in results");
                                continue;
                            }

                            try {
                                String userId = userSnapshot.getKey();
                                String displayName = userSnapshot.child("displayName").getValue(String.class);
                                String photoUrl = userSnapshot.child("photoUrl").getValue(String.class);
                                String email = userSnapshot.child("email").getValue(String.class);

                                Log.d(TAG, "Found user: " + displayName + " (" + email + ")");

                                if (displayName != null) {
                                    ChatContact contact = new ChatContact(userId, displayName, photoUrl, email);

                                    // Check if already connected or has pending request
                                    checkConnectionStatus(contact);

                                    users.add(contact);
                                    Log.d(TAG, "Added user to results: " + displayName);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user from search results", e);
                            }
                        }

                        // Now search by email
                        searchUsersByEmail(query);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error searching by name", error.toException());
                        searchUsersByEmail(query);
                    }
                });
    }

    private void searchUsersByEmail(String query) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        Log.d(TAG, "Searching by email with query: " + query);

        usersRef.orderByChild("email")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "Email search found " + snapshot.getChildrenCount() + " results");

                        List<ChatContact> emailResults = new ArrayList<>();

                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            // Skip current user
                            if (userSnapshot.getKey().equals(currentUserId)) continue;

                            try {
                                String userId = userSnapshot.getKey();
                                String displayName = userSnapshot.child("displayName").getValue(String.class);
                                String photoUrl = userSnapshot.child("photoUrl").getValue(String.class);
                                String email = userSnapshot.child("email").getValue(String.class);

                                Log.d(TAG, "Found user by email: " + displayName + " (" + email + ")");

                                if (displayName != null) {
                                    // Check if this user is already in results from name query
                                    boolean alreadyAdded = false;
                                    for (ChatContact existing : users) {
                                        if (existing.getUserId().equals(userId)) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }

                                    if (!alreadyAdded) {
                                        ChatContact contact = new ChatContact(userId, displayName, photoUrl, email);

                                        // Check if already connected or has pending request
                                        checkConnectionStatus(contact);

                                        emailResults.add(contact);
                                        Log.d(TAG, "Added user to email results: " + displayName);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user from email search results", e);
                            }
                        }

                        // Add email results to overall results
                        users.addAll(emailResults);

                        // Update UI
                        searchProgress.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();

                        // Show empty view if no results
                        if (users.isEmpty()) {
                            emptyResults.setText("No users found matching \"" + query + "\"");
                            emptyResults.setVisibility(View.VISIBLE);
                            Log.d(TAG, "No users found, showing empty view");
                        } else {
                            emptyResults.setVisibility(View.GONE);
                            Log.d(TAG, "Showing " + users.size() + " users in search results");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error searching by email", error.toException());
                        finishSearch();
                    }
                });
    }

    private void finishSearch() {
        searchProgress.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();

        if (users.isEmpty()) {
            emptyResults.setText("No users found");
            emptyResults.setVisibility(View.VISIBLE);
        } else {
            emptyResults.setVisibility(View.GONE);
        }
    }

    private void checkConnectionStatus(ChatContact contact) {
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("contacts")
                .child(contact.getUserId());

        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean connected = snapshot.child("connected").getValue(Boolean.class);
                    if (connected != null && connected) {
                        contact.setConnected(true);
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        // Check pending requests
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("outgoingRequests")
                .child(contact.getUserId());

        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    contact.setPendingRequest(true);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @Override
    public void onSendRequest(ChatContact contact) {
        Log.d(TAG, "Sending request to " + contact.getDisplayName());

        // Add to current user's outgoing requests
        DatabaseReference currentUserRequestRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUserId)
                .child("outgoingRequests")
                .child(contact.getUserId());

        currentUserRequestRef.setValue(contact);

        // Add to other user's incoming requests
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ChatContact currentUserContact = new ChatContact(
                    currentUser.getUid(),
                    currentUser.getDisplayName(),
                    currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null,
                    currentUser.getEmail()
            );

            currentUserContact.setPendingRequest(true);

            DatabaseReference otherUserRequestRef = FirebaseDatabase.getInstance().getReference("chats")
                    .child(contact.getUserId())
                    .child("requests")
                    .child(currentUser.getUid());

            otherUserRequestRef.setValue(currentUserContact)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Request sent to " + contact.getDisplayName(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Request sent successfully");

                        // Update the contact in the UI
                        contact.setPendingRequest(true);
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to send request", e);
                    });
        }
    }
}