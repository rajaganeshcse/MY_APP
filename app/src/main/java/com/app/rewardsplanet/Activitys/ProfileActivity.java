package com.app.rewardsplanet.Activitys;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.activity_delete_account;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private ImageView btnBack;
    private ImageView imgProfile;
    private ImageView btnEditPhoto;

    private EditText edtName;
    private EditText edtMobile;
    private EditText edtEmail;

    private TextView txtDob;
    private TextView txtProfileStrength;

    private ProgressBar profileProgress;

    private LinearLayout dateContainer;

    private RadioGroup radioGender;
    private RadioButton radioMale;
    private RadioButton radioFemale;

    private MaterialButton btnSave;
    private TextView btnDelete;

    // =========================================================
    // DATE
    // =========================================================

    private Calendar selectedDate;

    // =========================================================
    // IMAGE
    // =========================================================

    private static final int PICK_IMAGE = 1001;

    // =========================================================
    // FIREBASE
    // =========================================================

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        // Initialize views
        initViews();

        // Full screen
        makeFullScreen();

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

        // =====================================================
        // CHECK LOGIN
        // =====================================================

        if (currentUser != null) {

            uid = currentUser.getUid();

            loadUserData();

        } else {

            Toast.makeText(
                    this,
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            goToLogin();

            return;
        }

        // Setup listeners
        setupListeners();

        // Profile strength
        updateProfileStrength();
    }

    // =========================================================
    // INITIALIZE VIEWS
    // =========================================================

    private void initViews() {

        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        btnEditPhoto = findViewById(R.id.btnEditPhoto);

        edtName = findViewById(R.id.edtName);
        edtMobile = findViewById(R.id.edtMobile);
        edtEmail = findViewById(R.id.edtEmail);

        txtDob = findViewById(R.id.txtDob);
        txtProfileStrength = findViewById(R.id.txtProfileStrength);

        profileProgress = findViewById(R.id.profileProgress);

        dateContainer = findViewById(R.id.dateContainer);

        radioGender = findViewById(R.id.radioGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
    }

    // =========================================================
    // LOAD USER DATA
    // =========================================================

    private void loadUserData() {

        if (uid == null) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        // =====================================================
        // GOOGLE / FIREBASE PROFILE IMAGE
        // =====================================================

        if (currentUser != null && currentUser.getPhotoUrl() != null) {

            loadImage(currentUser.getPhotoUrl());

        } else {

            imgProfile.setImageResource(R.drawable.ic_profile);
        }

        // =====================================================
        // FIRESTORE USER DATA
        // =====================================================

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {

                        Toast.makeText(
                                ProfileActivity.this,
                                "User data not found",
                                Toast.LENGTH_SHORT
                        ).show();

                        updateProfileStrength();

                        return;
                    }

                    // =================================================
                    // NAME
                    // =================================================

                    String name = documentSnapshot.getString("name");

                    if (name != null && !name.trim().isEmpty()) {

                        edtName.setText(name);
                    }

                    // =================================================
                    // EMAIL
                    // =================================================

                    String email = documentSnapshot.getString("email");

                    if (email != null && !email.trim().isEmpty()) {

                        edtEmail.setText(email);
                    }

                    // =================================================
                    // MOBILE
                    // =================================================

                    String mobile = documentSnapshot.getString("mobile");

                    if (mobile != null && !mobile.trim().isEmpty()) {

                        edtMobile.setText(mobile);
                    }

                    // =================================================
                    // DOB
                    // =================================================

                    String dob = documentSnapshot.getString("dob");

                    if (dob != null && !dob.trim().isEmpty()) {

                        txtDob.setText(dob);
                    }

                    // =================================================
                    // GENDER
                    // =================================================

                    String gender = documentSnapshot.getString("gender");

                    if (gender != null) {

                        if (gender.equalsIgnoreCase("Male")) {

                            radioMale.setChecked(true);

                        } else if (gender.equalsIgnoreCase("Female")) {

                            radioFemale.setChecked(true);
                        }
                    }

                    // =================================================
                    // FIRESTORE PROFILE IMAGE
                    // =================================================

                    String profileImage =
                            documentSnapshot.getString("profileImage");

                    /*
                     * Use Firestore image only if Firebase Auth
                     * does not have a Google/Firebase photo.
                     */

                    if ((currentUser == null
                            || currentUser.getPhotoUrl() == null)
                            && profileImage != null
                            && !profileImage.trim().isEmpty()) {

                        loadImage(Uri.parse(profileImage));
                    }

                    // =================================================
                    // PROFILE STRENGTH
                    // =================================================

                    updateProfileStrength();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ProfileActivity.this,
                            "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    updateProfileStrength();
                });
    }

    // =========================================================
    // LOAD IMAGE
    // =========================================================

    private void loadImage(Uri imageUri) {

        if (imageUri == null) {

            imgProfile.setImageResource(R.drawable.ic_profile);

            return;
        }

        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);
    }

    // =========================================================
    // PROFILE STRENGTH
    // =========================================================
    //
    // NAME   = 20%
    // MOBILE = 20%
    // EMAIL  = 20%
    // DOB    = 20%
    // GENDER = 20%
    //
    // TOTAL = 100%
    // =========================================================

    private void updateProfileStrength() {

        if (edtName == null
                || edtMobile == null
                || edtEmail == null
                || txtDob == null
                || radioMale == null
                || radioFemale == null
                || profileProgress == null
                || txtProfileStrength == null) {

            return;
        }

        int completed = 0;
        int total = 5;

        // =====================================================
        // NAME
        // =====================================================

        String name = edtName.getText()
                .toString()
                .trim();

        if (!name.isEmpty()) {

            completed++;
        }

        // =====================================================
        // MOBILE
        // =====================================================

        String mobile = edtMobile.getText()
                .toString()
                .trim();

        if (!mobile.isEmpty()) {

            completed++;
        }

        // =====================================================
        // EMAIL
        // =====================================================

        String email = edtEmail.getText()
                .toString()
                .trim();

        if (!email.isEmpty()) {

            completed++;
        }

        // =====================================================
        // DOB
        // =====================================================

        String dob = txtDob.getText()
                .toString()
                .trim();

        if (!dob.isEmpty()
                && !dob.equalsIgnoreCase("Date of Birth")) {

            completed++;
        }

        // =====================================================
        // GENDER
        // =====================================================

        if (radioMale.isChecked()
                || radioFemale.isChecked()) {

            completed++;
        }

        // =====================================================
        // CALCULATE
        // =====================================================

        int percentage = (completed * 100) / total;

        // =====================================================
        // PROGRESS BAR
        // =====================================================

        profileProgress.setMax(100);
        profileProgress.setProgress(percentage);

        // =====================================================
        // TEXT
        // =====================================================

        txtProfileStrength.setText(
                percentage + "% COMPLETED"
        );
    }

    // =========================================================
    // SETUP LISTENERS
    // =========================================================

    private void setupListeners() {

        // =====================================================
        // BACK
        // =====================================================

        btnBack.setOnClickListener(v -> finish());

        // =====================================================
        // EDIT PHOTO
        // =====================================================

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

        // =====================================================
        // DATE
        // =====================================================

        dateContainer.setOnClickListener(
                v -> showDatePicker()
        );

        txtDob.setOnClickListener(
                v -> showDatePicker()
        );

        // =====================================================
        // GENDER
        // =====================================================

        radioMale.setOnClickListener(
                v -> updateProfileStrength()
        );

        radioFemale.setOnClickListener(
                v -> updateProfileStrength()
        );

        // =====================================================
        // NAME
        // =====================================================

        edtName.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateProfileStrength();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        // =====================================================
        // SAVE
        // =====================================================

        btnSave.setOnClickListener(
                v -> saveProfile()
        );

        // =====================================================
        // DELETE ACCOUNT
        // =====================================================

        btnDelete.setOnClickListener(v -> {

            Intent intent = new Intent(
                    ProfileActivity.this,
                    activity_delete_account.class
            );

            startActivity(intent);
        });
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

                            updateProfileStrength();
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
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

        // =====================================================
        // NAME
        // =====================================================

        String name = edtName.getText()
                .toString()
                .trim();

        if (name.isEmpty()) {

            edtName.setError("Enter your name");
            edtName.requestFocus();

            return;
        }

        // =====================================================
        // GENDER
        // =====================================================

        String gender = "";

        if (radioMale.isChecked()) {

            gender = "Male";

        } else if (radioFemale.isChecked()) {

            gender = "Female";
        }

        // =====================================================
        // DOB
        // =====================================================

        String dob = txtDob.getText()
                .toString()
                .trim();

        // =====================================================
        // UPDATE FIRESTORE
        // =====================================================

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

                    updateProfileStrength();
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
    // GO TO LOGIN
    // =========================================================

    private void goToLogin() {

        Intent intent = new Intent(
                ProfileActivity.this,
                activity_login.class
        );

        // =====================================================
        // REMOVE ALL PREVIOUS ACTIVITIES
        // =====================================================

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // FULL SCREEN
    // =========================================================

    private void makeFullScreen() {

        Window window = getWindow();

        // =====================================================
        // ANDROID 11+
        // =====================================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    window.getInsetsController();

            if (controller != null) {

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            // =================================================
            // OLD ANDROID
            // =================================================

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // =====================================================
        // STATUS BAR
        // =====================================================

        window.setStatusBarColor(Color.TRANSPARENT);

        // =====================================================
        // NAVIGATION BAR
        // =====================================================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    // =========================================================
    // IMAGE PICKER RESULT
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

        if (requestCode == PICK_IMAGE
                && resultCode == RESULT_OK
                && data != null) {

            Uri imageUri = data.getData();

            if (imageUri != null) {

                loadImage(imageUri);
            }
        }
    }
}
