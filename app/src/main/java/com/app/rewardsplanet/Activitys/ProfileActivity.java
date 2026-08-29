package com.app.rewardsplanet.Activitys;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView imgProfile;
    private ImageView btnEditPhoto;

    private EditText edtName;
    private EditText edtMobile;
    private EditText edtEmail;

    private TextView txtDob;
    private LinearLayout dateContainer;

    private RadioGroup radioGender;
    private RadioButton radioMale;
    private RadioButton radioFemale;

    private Button btnSave;
    private TextView btnDelete;

    private Calendar selectedDate;

    private static final int PICK_IMAGE = 1001;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Current user's UID
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        initViews();

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get logged-in user UID
        if (auth.getCurrentUser() != null) {

            uid = auth.getCurrentUser().getUid();

            loadUserData();

        } else {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        }

        setupListeners();
    }


    private void initViews() {

        btnBack = findViewById(R.id.btnBack);

        imgProfile = findViewById(R.id.imgProfile);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);

        edtName = findViewById(R.id.edtName);
        edtMobile = findViewById(R.id.edtMobile);
        edtEmail = findViewById(R.id.edtEmail);

        txtDob = findViewById(R.id.txtDob);
        dateContainer = findViewById(R.id.dateContainer);

        radioGender = findViewById(R.id.radioGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
    }


    // =========================================================
    // READ USER DATA FROM FIRESTORE
    // =========================================================

    private void loadUserData() {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        // Name
                        String name =
                                documentSnapshot.getString("name");

                        if (name != null) {
                            edtName.setText(name);
                        }


                        // Email
                        String email =
                                documentSnapshot.getString("email");

                        if (email != null) {
                            edtEmail.setText(email);
                        }


                        // Mobile
                        String mobile =
                                documentSnapshot.getString("mobile");

                        if (mobile != null) {
                            edtMobile.setText(mobile);
                        }


                        // DOB
                        String dob =
                                documentSnapshot.getString("dob");

                        if (dob != null && !dob.isEmpty()) {
                            txtDob.setText(dob);
                        }


                        // Gender
                        String gender =
                                documentSnapshot.getString("gender");

                        if (gender != null) {

                            if (gender.equalsIgnoreCase("Male")) {

                                radioMale.setChecked(true);

                            } else if (gender.equalsIgnoreCase("Female")) {

                                radioFemale.setChecked(true);
                            }
                        }


                        // Read coins
                        Long coins =
                                documentSnapshot.getLong("coins");

                        if (coins != null) {

                            // Example:
                            // 621

                            long userCoins = coins;

                            // You can use userCoins wherever required.
                            // Example:
                            // txtCoins.setText(userCoins + " Coins");
                        }


                    } else {

                        Toast.makeText(
                                ProfileActivity.this,
                                "User data not found",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private void setupListeners() {

        // Back
        btnBack.setOnClickListener(v -> finish());


        // Edit profile photo
        btnEditPhoto.setOnClickListener(v -> {

            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            );

            startActivityForResult(
                    intent,
                    PICK_IMAGE
            );
        });


        // Date picker
        dateContainer.setOnClickListener(
                v -> showDatePicker()
        );

        txtDob.setOnClickListener(
                v -> showDatePicker()
        );


        // Save
        btnSave.setOnClickListener(
                v -> saveProfile()
        );


        // Delete
        btnDelete.setOnClickListener(
                v -> showDeleteDialog()
        );
    }


    // =========================================================
    // DATE PICKER
    // =========================================================

    private void showDatePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,

                        (view, year, month, dayOfMonth) -> {

                            selectedDate =
                                    Calendar.getInstance();

                            selectedDate.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            SimpleDateFormat sdf =
                                    new SimpleDateFormat(
                                            "dd-MMMM-yyyy",
                                            Locale.ENGLISH
                                    );

                            txtDob.setText(
                                    sdf.format(
                                            selectedDate.getTime()
                                    )
                            );

                        },

                        calendar.get(
                                Calendar.YEAR
                        ),

                        calendar.get(
                                Calendar.MONTH
                        ),

                        calendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        datePickerDialog.show();
    }


    // =========================================================
    // SAVE PROFILE
    // =========================================================

    private void saveProfile() {

        if (uid == null) {
            return;
        }


        String name =
                edtName.getText()
                        .toString()
                        .trim();


        if (name.isEmpty()) {

            edtName.setError(
                    "Enter your name"
            );

            edtName.requestFocus();

            return;
        }


        // Gender
        String gender = "";

        if (radioMale.isChecked()) {

            gender = "Male";

        } else if (radioFemale.isChecked()) {

            gender = "Female";
        }


        // DOB
        String dob =
                txtDob.getText()
                        .toString()
                        .trim();


        /*
         * IMPORTANT:
         *
         * update() changes only these fields.
         *
         * Existing fields such as:
         *
         * coins
         * tickets
         * dailySpinCount
         * streak_count
         * referralCode
         * game_ids
         *
         * will NOT be deleted.
         */

        db.collection("users")
                .document(uid)
                .update(
                        "name", name,
                        "dob", dob,
                        "gender", gender
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Profile saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =========================================================
    // READ COINS
    // =========================================================

    private void readCoins() {

        if (uid == null) {
            return;
        }

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        Long coins =
                                documentSnapshot.getLong("coins");

                        if (coins != null) {

                            long userCoins = coins;

                            // Example:
                            // txtCoins.setText(userCoins + " Coins");

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Coins: " + userCoins,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }


    // =========================================================
    // ADD / SET COINS
    // =========================================================

    private void setCoins(long amount) {

        if (uid == null) {
            return;
        }

        db.collection("users")
                .document(uid)
                .update(
                        "coins",
                        amount
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Coins updated",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Coins update failed",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    // =========================================================
    // ADD COINS
    // =========================================================

    private void addCoins(long amount) {

        if (uid == null) {
            return;
        }

        db.collection("users")
                .document(uid)
                .update(
                        "coins",
                        com.google.firebase.firestore.FieldValue
                                .increment(amount)
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "+" + amount + " Coins",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to add coins",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }


    // =========================================================
    // DELETE ACCOUNT
    // =========================================================

    private void showDeleteDialog() {

        AlertDialog dialog =
                new AlertDialog.Builder(this)

                        .setTitle("Delete Account")

                        .setMessage(
                                "Are you sure you want to delete " +
                                        "your account? This action cannot " +
                                        "be undone."
                        )

                        .setNegativeButton(
                                "Cancel",
                                null
                        )

                        .setPositiveButton(
                                "Delete",
                                (dialogInterface, which) ->
                                        deleteAccount()
                        )

                        .create();

        dialog.show();

        dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                )
                .setTextColor(Color.RED);
    }


    private void deleteAccount() {

        if (uid == null) {
            return;
        }


        db.collection("users")
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> {

                    if (auth.getCurrentUser() != null) {

                        auth.getCurrentUser()
                                .delete()
                                .addOnSuccessListener(aVoid -> {

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Account deleted",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    finish();

                                })
                                .addOnFailureListener(e -> {

                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Account deletion failed",
                                            Toast.LENGTH_LONG
                                    ).show();
                                });

                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to delete data",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    // =========================================================
    // PROFILE IMAGE
    // =========================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            @Nullable Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == PICK_IMAGE &&
                resultCode == RESULT_OK &&
                data != null) {

            Uri imageUri = data.getData();

            if (imageUri != null) {

                imgProfile.setImageURI(
                        imageUri
                );
            }
        }
    }
}