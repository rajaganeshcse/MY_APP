package com.app.rewardsplanet.Fragements;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.app.rewardsplanet.Activitys.activity_login;
import com.app.rewardsplanet.R;
import com.app.rewardsplanet.invite.activity_refer_earn;
import com.app.rewardsplanet.withdraws.RewardHistory;
import com.app.rewardsplanet.withdraws.TransactionHistoryActivity;
import com.app.rewardsplanet.UserPref;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    // ================= UI =================
    private ImageView imgProfile,btnCopy;
    private TextView txtName, txtUid, txtCoins, txtTickets,getTxtUid;


    private LinearLayout btnAccountHistory, btnMyRewards, btnHelp,invite,
            btnPrivacy, btnTerms, btnLogout,tickethistory,coinhistory;

    private TextView btnFacebook, btnInstagram, btnTelegram, btnYoutube;

    // ================= LOCAL STORAGE =================
    private UserPref userPref;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userPref = new UserPref(requireContext());

        initViews(view);
        loadUserFromPref();
        setClicks();

        return view;
    }

    private void initViews(View view) {

        imgProfile = view.findViewById(R.id.imgProfile);
        txtName = view.findViewById(R.id.txtName);
        txtUid = view.findViewById(R.id.txtUid);
        txtCoins = view.findViewById(R.id.txtCoins);
        txtTickets = view.findViewById(R.id.txtTickets);

        btnAccountHistory = view.findViewById(R.id.btnAccountHistory);
        btnMyRewards = view.findViewById(R.id.btnMyRewards);
        btnHelp = view.findViewById(R.id.btnHelp);
        btnPrivacy = view.findViewById(R.id.btnPrivacy);
        btnTerms = view.findViewById(R.id.btnTerms);
        btnLogout = view.findViewById(R.id.btnLogout);
        invite = view.findViewById(R.id.invite);
        getTxtUid = view.findViewById(R.id.txtUid);
        btnCopy = view.findViewById(R.id.btnCopy);
        tickethistory = view.findViewById(R.id.tickethistory);
        coinhistory = view.findViewById(R.id.coinhistory);


        btnFacebook = view.findViewById(R.id.btnFacebook);
        btnInstagram = view.findViewById(R.id.btnInstagram);
        btnTelegram = view.findViewById(R.id.btnTelegram);
        btnYoutube = view.findViewById(R.id.btnYoutube);

        tickethistory.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    RewardHistory.class
            );
            startActivity(intent);
        });
        coinhistory.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    RewardHistory.class
            );
            startActivity(intent);
        });
    }
    private void loadUserFromPref() {

        txtName.setText(userPref.getName());

        // 🔥 SHOW REFERRAL CODE INSTEAD OF UID
        txtUid.setText("Referral Code: " + userPref.getReferralCode());

        txtCoins.setText(userPref.getCoins() + " Coins");
        txtTickets.setText(userPref.getWalletToken() + " Tickets");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(imgProfile);
        }
    }

    private void copyCode() {
        ClipboardManager cm =
                (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);

        cm.setPrimaryClip(
                ClipData.newPlainText(
                        "referral",
                        txtUid.getText().toString().substring(15)
                )
        );

        Toast.makeText(getContext(), "Code copied", Toast.LENGTH_SHORT).show();

    }

    /**
     * ================= CLICK HANDLERS =================
     */
    private void setClicks() {
        btnCopy.setOnClickListener(v -> copyCode());

        btnAccountHistory.setOnClickListener(v ->{copyCode();}
        );

        btnAccountHistory.setOnClickListener(v ->{
                    Intent intent = new Intent(
                            requireContext(),
                            TransactionHistoryActivity.class
                    );
                    startActivity(intent);
                }
        );

        btnMyRewards.setOnClickListener(v ->{
            Intent intent = new Intent(
                    requireContext(),
                    RewardHistory.class
            );
            startActivity(intent);
        });
        invite.setOnClickListener(v ->{
            Intent intent = new Intent(
                    requireContext(),
                    activity_refer_earn.class
            );
            startActivity(intent);});

        btnHelp.setOnClickListener(v ->
                openUrl("https://yourdomain.com/help")
        );

        btnPrivacy.setOnClickListener(v ->
                openUrl("https://yourdomain.com/privacy")
        );

        btnTerms.setOnClickListener(v ->
                openUrl("https://yourdomain.com/terms")
        );

        btnLogout.setOnClickListener(v -> {

            // 🔥 Clear local data
            userPref.logout();

            // 🔥 Firebase logout
            FirebaseAuth.getInstance().signOut();

            // 🔥 Google logout
            GoogleSignInClient googleSignInClient =
                    GoogleSignIn.getClient(
                            requireContext(),
                            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    );

            googleSignInClient.signOut();

            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), activity_login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // ================= SOCIAL LINKS =================
        btnFacebook.setOnClickListener(v ->
                openUrl("https://t.me/+aTdeGXEDpt84ZDVl")
        );

        btnInstagram.setOnClickListener(v ->
                openUrl("https://t.me/+aTdeGXEDpt84ZDVl")
        );

        btnTelegram.setOnClickListener(v ->
                openUrl("https://t.me/+aTdeGXEDpt84ZDVl")
        );

        btnYoutube.setOnClickListener(v ->
                openUrl("https://t.me/+aTdeGXEDpt84ZDVl")
        );
    }

    /**
     * ================= OPEN LINK SAFELY =================
     */
    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
}
