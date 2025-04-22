package com.aryan.edenic;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";

    // UI components
    private ImageView profileImage;
    private EditText firstNameInput;
    private EditText lastNameInput;
    private TextView emailDisplay;
    private TextView memberSince;
    private TextView portfolioValue;
    private TextView totalTrades;
    private TextView leaderboardPosition;
    private Button changePhotoButton;
    private Button saveButton;
    private Button logoutButton;
    private ImageButton backButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userRef;

    // Photo picker launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        updateProfilePhoto(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            // User not logged in, redirect to login
            startActivity(new Intent(this, Auth_Activity.class));
            finish();
            return;
        }

        // Initialize Firebase Database reference
        userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        // Initialize UI components
        initializeViews();
        setupListeners();
        loadUserData();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        firstNameInput = findViewById(R.id.first_name_input);
        lastNameInput = findViewById(R.id.last_name_input);
        emailDisplay = findViewById(R.id.email_display);
        memberSince = findViewById(R.id.member_since);
        portfolioValue = findViewById(R.id.portfolio_value);
        totalTrades = findViewById(R.id.total_trades);
        leaderboardPosition = findViewById(R.id.leaderboard_position);
        changePhotoButton = findViewById(R.id.change_photo_button);
        saveButton = findViewById(R.id.save_button);
        logoutButton = findViewById(R.id.logout_button);
        backButton = findViewById(R.id.back_button);
    }

    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Change photo button
        changePhotoButton.setOnClickListener(v -> openImagePicker());

        // Save button
        saveButton.setOnClickListener(v -> saveUserProfile());

        // Logout button
        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void loadUserData() {
        // Load user profile photo
        if (user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(profileImage);
        }

        // Load name from Firebase Auth
        String displayName = user.getDisplayName();
        if (displayName != null) {
            String[] names = displayName.split(" ", 2);
            firstNameInput.setText(names[0]);
            if (names.length > 1) {
                lastNameInput.setText(names[1]);
            }
        }

        // Set email (non-editable)
        emailDisplay.setText(user.getEmail());

        // Load additional user data from Firebase Database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Load member since date
                    if (snapshot.child("lastLogin").exists()) {
                        long timestamp = snapshot.child("lastLogin").getValue(Long.class);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                        memberSince.setText(dateFormat.format(new Date(timestamp)));
                    }

                    // Load portfolio value
                    if (snapshot.child("portfolioValue").exists()) {
                        double value = snapshot.child("portfolioValue").getValue(Double.class);
                        portfolioValue.setText(String.format(Locale.US, "$%,.2f", value));
                    }

                    // Count total trades
                    loadTotalTrades();

                    // Load leaderboard position
                    loadLeaderboardPosition();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading user data", error.toException());
                Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTotalTrades() {
        FirebaseDatabase.getInstance().getReference("transactions")
                .child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        totalTrades.setText(String.valueOf(count));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error counting transactions", error.toException());
                    }
                });
    }

    private void loadLeaderboardPosition() {
        FirebaseDatabase.getInstance().getReference("leaderboard")
                .orderByChild("portfolioValue")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Create a list to store all entries
                        List<DataSnapshot> entries = new ArrayList<>();

                        // Add all entries to the list
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            entries.add(userSnapshot);
                        }

                        // Sort in descending order by portfolio value (highest first)
                        Collections.sort(entries, (a, b) -> {
                            double valueA = a.child("portfolioValue").getValue(Double.class);
                            double valueB = b.child("portfolioValue").getValue(Double.class);
                            return Double.compare(valueB, valueA); // Note the reversed order for descending
                        });

                        // Find current user's position
                        int position = 1;
                        for (DataSnapshot entry : entries) {
                            if (entry.getKey().equals(user.getUid())) {
                                leaderboardPosition.setText("#" + position);
                                return;
                            }
                            position++;
                        }

                        // If user not found in leaderboard
                        leaderboardPosition.setText("N/A");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error determining leaderboard position", error.toException());
                        leaderboardPosition.setText("--");
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void updateProfilePhoto(Uri photoUri) {
        // Show the image in the UI immediately
        Glide.with(this)
                .load(photoUri)
                .circleCrop()
                .into(profileImage);

        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update database reference as well
                    userRef.child("photoUrl").setValue(photoUri.toString());
                    Toast.makeText(ProfileActivity.this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile photo", e);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile photo", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();

        if (firstName.isEmpty()) {
            firstNameInput.setError("First name cannot be empty");
            return;
        }

        String fullName = lastName.isEmpty() ? firstName : firstName + " " + lastName;

        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update in Firebase Database
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("displayName", fullName);
                    updates.put("firstName", firstName);
                    updates.put("lastName", lastName);

                    userRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                                // Also update in leaderboard
                                FirebaseDatabase.getInstance().getReference("leaderboard")
                                        .child(user.getUid())
                                        .child("name")
                                        .setValue(fullName);

                                FirebaseDatabase.getInstance().getReference("leaderboard")
                                        .child(user.getUid())
                                        .child("displayName")
                                        .setValue(fullName);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating user in database", e);
                                Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutUser() {
        // Sign out from Firebase
        auth.signOut();

        // Sign out from Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Redirect to login screen
            Intent intent = new Intent(ProfileActivity.this, Auth_Activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}