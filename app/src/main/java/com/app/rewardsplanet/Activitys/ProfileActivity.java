package com.app.rewardsplanet.Activitys;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.activity_delete_account;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.repository.UserRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    private ImageView imgDobChevron;

    private ProgressBar profileProgress;

    private LinearLayout dateContainer;

    private LinearLayout genderContainer;
    private TextView txtGenderSymbol;
    private TextView txtGender;
    private ImageView imgGenderChevron;

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
        imgDobChevron = findViewById(R.id.imgDobChevron);

        genderContainer = findViewById(R.id.genderContainer);
        txtGenderSymbol = findViewById(R.id.txtGenderSymbol);
        txtGender = findViewById(R.id.txtGender);
        imgGenderChevron = findViewById(R.id.imgGenderChevron);

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

        UserRepository.getInstance(this)
                .getUser()
                .observe(this, user -> {

                    if (user == null) return;

                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        edtName.setText(user.getName());
                    }

                    if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                        edtMobile.setText(user.getPhone());
                    }

                    if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                        edtEmail.setText(user.getEmail());
                    }

                    if (user.getDob() != null && !user.getDob().trim().isEmpty()) {
                        txtDob.setText(user.getDob());
                    }

                    String gender = user.getGender();
                    if (gender != null && !gender.trim().isEmpty()) {
                        txtGender.setText(gender);
                    } else {
                        txtGender.setText("Male");
                    }
                    updateGenderSymbol(txtGender.getText().toString());

                    if (user.getProfile_pic() != null && !user.getProfile_pic().trim().isEmpty()) {
                        loadImage(Uri.parse(user.getProfile_pic()));
                    } else {
                        FirebaseUser currentUser = auth.getCurrentUser();
                        if (currentUser != null && currentUser.getPhotoUrl() != null) {
                            loadImage(currentUser.getPhotoUrl());
                        } else {
                            imgProfile.setImageResource(R.drawable.ic_profile);
                        }
                    }

                    updateProfileStrength();
                });
    }

    private void updateGenderSymbol(String gender) {
        if (txtGenderSymbol == null) return;
        if ("Female".equalsIgnoreCase(gender)) {
            txtGenderSymbol.setText("♀");
        } else if ("Male".equalsIgnoreCase(gender)) {
            txtGenderSymbol.setText("♂");
        } else {
            txtGenderSymbol.setText("👤");
        }
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
                || txtGender == null
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

        String gender = txtGender.getText()
                .toString()
                .trim();

        if (!gender.isEmpty()) {

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
        // GENDER DROPDOWN
        // =====================================================

        if (genderContainer != null) {
            genderContainer.setOnClickListener(
                    v -> showGenderDropdown()
            );
        }

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

        edtMobile.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        updateProfileStrength();
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
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
    // GENDER SELECTION CENTERED DIALOG
    // =========================================================

    private Dialog genderDialog;

    private void showGenderDropdown() {

        if (genderDialog != null && genderDialog.isShowing()) {
            genderDialog.dismiss();
            return;
        }

        genderDialog = new Dialog(this);
        genderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        genderDialog.setCancelable(true);
        genderDialog.setCanceledOnTouchOutside(true);

        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_gender_select, null);
        genderDialog.setContentView(popupView);

        if (genderDialog.getWindow() != null) {
            genderDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            genderDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            genderDialog.getWindow().setGravity(Gravity.CENTER);
        }

        LinearLayout optionMale = popupView.findViewById(R.id.optionMale);
        LinearLayout optionFemale = popupView.findViewById(R.id.optionFemale);
        LinearLayout optionOther = popupView.findViewById(R.id.optionOther);

        ImageView checkMale = popupView.findViewById(R.id.checkMale);
        ImageView checkFemale = popupView.findViewById(R.id.checkFemale);
        ImageView checkOther = popupView.findViewById(R.id.checkOther);

        String currentGender = txtGender.getText().toString().trim();

        if (checkMale != null && checkFemale != null && checkOther != null) {
            checkMale.setVisibility("Male".equalsIgnoreCase(currentGender) ? View.VISIBLE : View.GONE);
            checkFemale.setVisibility("Female".equalsIgnoreCase(currentGender) ? View.VISIBLE : View.GONE);
            checkOther.setVisibility("Rather not to say".equalsIgnoreCase(currentGender) ? View.VISIBLE : View.GONE);
        }

        if (optionMale != null) {
            optionMale.setBackgroundResource("Male".equalsIgnoreCase(currentGender) ? R.drawable.bg_gender_option_selected : R.drawable.bg_chip_light);
            optionMale.setOnClickListener(v -> {
                txtGender.setText("Male");
                updateGenderSymbol("Male");
                updateProfileStrength();
                genderDialog.dismiss();
            });
        }

        if (optionFemale != null) {
            optionFemale.setBackgroundResource("Female".equalsIgnoreCase(currentGender) ? R.drawable.bg_gender_option_selected : R.drawable.bg_chip_light);
            optionFemale.setOnClickListener(v -> {
                txtGender.setText("Female");
                updateGenderSymbol("Female");
                updateProfileStrength();
                genderDialog.dismiss();
            });
        }

        if (optionOther != null) {
            optionOther.setBackgroundResource("Rather not to say".equalsIgnoreCase(currentGender) ? R.drawable.bg_gender_option_selected : R.drawable.bg_chip_light);
            optionOther.setOnClickListener(v -> {
                txtGender.setText("Rather not to say");
                updateGenderSymbol("Rather not to say");
                updateProfileStrength();
                genderDialog.dismiss();
            });
        }

        if (imgGenderChevron != null) {
            imgGenderChevron.setImageResource(R.drawable.ic_chevron_up);
        }

        genderDialog.setOnDismissListener(dialogInterface -> {
            if (imgGenderChevron != null) {
                imgGenderChevron.setImageResource(R.drawable.ic_chevron_down);
            }
        });

        genderDialog.show();
    }

    // =========================================================
    // DATE PICKER CENTERED DIALOG
    // =========================================================

    private Dialog dobDialog;

    private static final String[] MONTH_NAMES = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private void showDatePicker() {

        if (dobDialog != null && dobDialog.isShowing()) {
            dobDialog.dismiss();
            return;
        }

        dobDialog = new Dialog(this);
        dobDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dobDialog.setCancelable(true);
        dobDialog.setCanceledOnTouchOutside(true);

        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_dob_select, null);
        dobDialog.setContentView(popupView);

        if (dobDialog.getWindow() != null) {
            dobDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dobDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dobDialog.getWindow().setGravity(Gravity.CENTER);
        }

        NumberPicker npDay = popupView.findViewById(R.id.npDay);
        NumberPicker npMonth = popupView.findViewById(R.id.npMonth);
        NumberPicker npYear = popupView.findViewById(R.id.npYear);

        TextView btnCancelDob = popupView.findViewById(R.id.btnCancelDob);
        View btnConfirmDob = popupView.findViewById(R.id.btnConfirmDob);

        // Configure Month Picker
        npMonth.setMinValue(0);
        npMonth.setMaxValue(MONTH_NAMES.length - 1);
        npMonth.setDisplayedValues(MONTH_NAMES);
        npMonth.setWrapSelectorWheel(true);

        // Configure Year Picker
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        npYear.setMinValue(1950);
        npYear.setMaxValue(currentYear);
        npYear.setWrapSelectorWheel(false);

        // Configure Day Picker
        npDay.setMinValue(1);
        npDay.setMaxValue(31);
        npDay.setFormatter(value -> String.format(Locale.ENGLISH, "%02d", value));
        npDay.setWrapSelectorWheel(true);

        // Dynamic day limit on month/year change
        NumberPicker.OnValueChangeListener dateChangeListener = (picker, oldVal, newVal) -> {
            int year = npYear.getValue();
            int month = npMonth.getValue();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            npDay.setMaxValue(maxDays);
            if (npDay.getValue() > maxDays) {
                npDay.setValue(maxDays);
            }
        };

        npMonth.setOnValueChangedListener(dateChangeListener);
        npYear.setOnValueChangedListener(dateChangeListener);

        // Set initial values from txtDob if available
        String currentDob = txtDob.getText().toString().trim();
        int initialDay = 7;
        int initialMonth = 8; // September
        int initialYear = 2005;

        if (currentDob.contains("-")) {
            String[] parts = currentDob.split("-");
            if (parts.length == 3) {
                try {
                    initialDay = Integer.parseInt(parts[0].trim());
                } catch (Exception ignored) {}

                String mStr = parts[1].trim();
                for (int i = 0; i < MONTH_NAMES.length; i++) {
                    if (MONTH_NAMES[i].equalsIgnoreCase(mStr) || MONTH_NAMES[i].toLowerCase().startsWith(mStr.toLowerCase())) {
                        initialMonth = i;
                        break;
                    }
                }

                try {
                    initialYear = Integer.parseInt(parts[2].trim());
                } catch (Exception ignored) {}
            }
        }

        if (initialYear < 1950 || initialYear > currentYear) initialYear = 2000;
        npYear.setValue(initialYear);
        npMonth.setValue(initialMonth);

        Calendar initCal = Calendar.getInstance();
        initCal.set(Calendar.YEAR, initialYear);
        initCal.set(Calendar.MONTH, initialMonth);
        int maxDaysForInit = initCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        npDay.setMaxValue(maxDaysForInit);
        if (initialDay > maxDaysForInit) initialDay = maxDaysForInit;
        npDay.setValue(initialDay);

        if (btnCancelDob != null) {
            btnCancelDob.setOnClickListener(v -> dobDialog.dismiss());
        }

        if (btnConfirmDob != null) {
            btnConfirmDob.setOnClickListener(v -> {
                int day = npDay.getValue();
                int month = npMonth.getValue();
                int year = npYear.getValue();

                selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, day);

                String formattedDob = String.format(Locale.ENGLISH, "%02d-%s-%d", day, MONTH_NAMES[month], year);
                txtDob.setText(formattedDob);
                updateProfileStrength();
                dobDialog.dismiss();
            });
        }

        if (imgDobChevron != null) {
            imgDobChevron.setImageResource(R.drawable.ic_chevron_up);
        }

        dobDialog.setOnDismissListener(dialogInterface -> {
            if (imgDobChevron != null) {
                imgDobChevron.setImageResource(R.drawable.ic_chevron_down);
            }
        });

        dobDialog.show();
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

        String mobile = edtMobile.getText()
                .toString()
                .trim();

        if (!mobile.isEmpty() && mobile.length() != 10) {
            edtMobile.setError("Mobile number must be 10 digits");
            edtMobile.requestFocus();
            return;
        }

        // =====================================================
        // GENDER
        // =====================================================

        String gender = txtGender.getText()
                .toString()
                .trim();

        // =====================================================
        // DOB
        // =====================================================

        String dob = txtDob.getText()
                .toString()
                .trim();

        // =====================================================
        // UPDATE FIRESTORE
        // =====================================================

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", mobile);
        updates.put("dob", dob);
        updates.put("gender", gender);

        final String finalGender = gender;
        final String finalDob = dob;

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // 1. Sync local UserPref
                    UserPref userPref = new UserPref(ProfileActivity.this);
                    userPref.setName(name);
                    userPref.setPhone(mobile);
                    userPref.setGender(finalGender);
                    userPref.setDob(finalDob);

                    // 2. Sync central UserRepository state
                    UserModel currentUserModel = UserRepository.getInstance(ProfileActivity.this).getCurrentUserModel();
                    if (currentUserModel != null) {
                        currentUserModel.setName(name);
                        currentUserModel.setPhone(mobile);
                        currentUserModel.setGender(finalGender);
                        currentUserModel.setDob(finalDob);
                        UserRepository.getInstance(ProfileActivity.this).updateLocalUser(currentUserModel);
                    } else {
                        UserRepository.getInstance(ProfileActivity.this).refreshCurrentUser();
                    }

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
