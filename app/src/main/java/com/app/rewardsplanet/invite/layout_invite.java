package com.app.rewardsplanet.invite;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.profile.ReferralUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class layout_invite extends Fragment {

    // ================= UI =================
    TextView txtCode;
    EditText edtReferral;
    ImageView btnCopy, btnWhatsapp, btnTelegram,
            btnFacebook, btnMessenger, btnShareAll;
    View btnValidate;

    // ================= FIREBASE =================
    FirebaseFirestore db;
    String uid;

    // ================= LOCAL CACHE =================
    UserPref userPref;

    private static final int REFERRAL_COIN_REWARD = 250;
    private static final int REFERRAL_TICKET_REWARD = 10;

    @Nullable
    @Override
    public View onCreateView(

            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                com.app.rewardsplanet.R.layout.activity_layout_invite,
                container,
                false
        );
        makeFullScreen();
        // UI
        txtCode = view.findViewById(com.app.rewardsplanet.R.id.txtReferralCode);
        edtReferral = view.findViewById(com.app.rewardsplanet.R.id.edtReferral);
        btnCopy = view.findViewById(com.app.rewardsplanet.R.id.btnCopy);
        btnValidate = view.findViewById(com.app.rewardsplanet.R.id.btnValidate);

        btnWhatsapp = view.findViewById(com.app.rewardsplanet.R.id.btnWhatsapp);
        btnTelegram = view.findViewById(com.app.rewardsplanet.R.id.btnTelegram);
        btnFacebook = view.findViewById(com.app.rewardsplanet.R.id.btnFacebook);
        btnMessenger = view.findViewById(com.app.rewardsplanet.R.id.btnMessenger);
        btnShareAll = view.findViewById(R.id.btnShareAll);

        // Firebase
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();

        // Local
        userPref = new UserPref(requireContext());

        if (uid == null) {
            toast("User not logged in");
            return view;
        }

        loadReferralCode();

        btnCopy.setOnClickListener(v -> copyCode());
        btnValidate.setOnClickListener(v -> validateReferral());

        btnWhatsapp.setOnClickListener(v -> shareToApp("com.whatsapp"));
        btnTelegram.setOnClickListener(v -> shareToApp("org.telegram.messenger"));
        btnFacebook.setOnClickListener(v -> shareToApp("com.facebook.katana"));
        btnMessenger.setOnClickListener(v -> shareToApp("com.facebook.orca"));
        btnShareAll.setOnClickListener(v -> shareAll());

        return view;
    }
    private void makeFullScreen() {
        Window window = getActivity().getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.parseColor("#ffffff"));
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.parseColor("#ffffff"));

        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }


    // ================= REFERRAL CODE =================

    private void loadReferralCode() {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    UserModel user = doc.toObject(UserModel.class);

                    if (user == null || user.getReferralCode() == null) {

                        String code = ReferralUtil.generateCode();

                        Map<String, Object> map = new HashMap<>();
                        map.put("referralCode", code);

                        db.collection("users")
                                .document(uid)
                                .set(map, SetOptions.merge());

                        txtCode.setText(code);
                    } else {
                        txtCode.setText(user.getReferralCode());
                    }
                });
    }

    // ================= COPY =================

    private void copyCode() {
        ClipboardManager cm =
                (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);

        cm.setPrimaryClip(
     ClipData.newPlainText(
                        "referral",
                        txtCode.getText().toString()
                )
        );

        toast("Code copied");
    }

    // ================= VALIDATE =================

    private void validateReferral() {

        String code = edtReferral.getText().toString().trim();

        if (code.isEmpty()) {
            toast("Enter referral code");
            return;
        }

        btnValidate.setEnabled(false);

        db.collection("users")
                .whereEqualTo("referralCode", code)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {

                    if (qs.isEmpty()) {
                        btnValidate.setEnabled(true);
                        toast("Invalid referral code");
                        return;
                    }

                    DocumentSnapshot refDoc = qs.getDocuments().get(0);
                    String refUid = refDoc.getId();

                    if (refUid.equals(uid)) {
                        btnValidate.setEnabled(true);
                        toast("Can't use your own code");
                        return;
                    }

                    applyReferral(refUid);
                })
                .addOnFailureListener(e -> {
                    btnValidate.setEnabled(true);
                    toast("Something went wrong");
                });
    }

    // ================= APPLY REFERRAL =================

    private void applyReferral(String refUid) {

        DocumentReference userRef =
                db.collection("users").document(uid);

        DocumentReference referrerRef =
                db.collection("users").document(refUid);

        userRef.get().addOnSuccessListener(doc -> {

            UserModel user = doc.toObject(UserModel.class);

            if (user == null) {
                btnValidate.setEnabled(true);
                return;
            }

            if (user.isReferralUsed()) {
                btnValidate.setEnabled(true);
                toast("Referral already used");
                return;
            }

            WriteBatch batch = db.batch();

            // -------- NEW USER --------
            batch.update(userRef,
                    "referredBy", refUid,
                    "referralUsed", true,
                    "coins", FieldValue.increment(REFERRAL_COIN_REWARD),
                    "tickets", FieldValue.increment(REFERRAL_TICKET_REWARD)
            );

            // -------- REFERRER --------
            Map<String, Object> referralUser = new HashMap<>();
            referralUser.put("userId", uid);
            referralUser.put("joinedAt", System.currentTimeMillis());

            batch.update(referrerRef,
                    "totalReferralCoins",
                    FieldValue.increment(REFERRAL_COIN_REWARD),
                    "totalReferralTickets",
                    FieldValue.increment(REFERRAL_TICKET_REWARD),
                    "referralUsers." + uid,
                    referralUser
            );

            batch.commit()
                    .addOnSuccessListener(unused -> {

                        // Update local cache
                        long currentCoins = userPref.getCoins();
                        userPref.setCoins(currentCoins + REFERRAL_COIN_REWARD);

                        toast("Referral applied 🎉");
                        btnValidate.setEnabled(false);
                    })
                    .addOnFailureListener(e -> {
                        btnValidate.setEnabled(true);
                        toast("Failed to apply referral");
                    });
        });
    }

    // ================= SHARE =================

    private String getShareMessage() {
        return "🎮 Join Gamex play & earn FREE coins!\n\n"+
                "🎁 Get ₹20 bonus instantly when you sign up with my link\n\n"+
                "⚡ Play games, complete simple tasks & earn real cash\n\n"+
                "✅ Withdraw easily once your wallet hits just ₹60.0\n\n"
                + "Use my referral code: " + txtCode.getText().toString()
                + "\n\nDownload now 👇\n"
                + "🔗 "+"https://play.google.com/store/apps/details?id="
                + requireContext().getPackageName();
    }

    private void shareToApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getShareMessage());
        intent.setPackage(packageName);

        try {
            startActivity(intent);
        } catch (Exception e) {
            toast("App not installed");
        }
    }

    private void shareAll() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getShareMessage());
        startActivity(Intent.createChooser(intent, "Invite using"));
    }

    // ================= TOAST =================

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
