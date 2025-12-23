package com.example.rgamer;

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

public class ProfileFragment extends Fragment {

    // UI
    private ImageView imgProfile;
    private TextView txtName, txtUid, txtCoins, txtTickets;

    private LinearLayout btnAccountHistory, btnMyRewards, btnHelp,
            btnPrivacy, btnTerms, btnLogout;

    private TextView btnFacebook, btnInstagram, btnTelegram, btnYoutube;

    // Local storage
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

        btnFacebook = view.findViewById(R.id.btnFacebook);
        btnInstagram = view.findViewById(R.id.btnInstagram);
        btnTelegram = view.findViewById(R.id.btnTelegram);
        btnYoutube = view.findViewById(R.id.btnYoutube);
    }

    /**
     * 🔥 Load data from UserPref
     */
    private void loadUserFromPref() {

        txtName.setText(userPref.getName());
        txtUid.setText("UID: " + userPref.getUid());

        txtCoins.setText(userPref.getCoins() + " Coins");
        txtTickets.setText(userPref.getTickets() + " Tickets");

        String profileUrl = userPref.getProfileImage();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(imgProfile);
        }
    }

    /**
     * 🔘 Click handlers
     */
    private void setClicks() {

        btnAccountHistory.setOnClickListener(v ->
                Toast.makeText(getContext(), "Account History", Toast.LENGTH_SHORT).show()
        );

        btnMyRewards.setOnClickListener(v ->
                Toast.makeText(getContext(), "My Rewards", Toast.LENGTH_SHORT).show()
        );

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
            userPref.logout();
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();

            // Redirect to Login
            startActivity(new Intent(getActivity(),activity_login.class));
            requireActivity().finish();
        });

        // 🌐 Social links
        btnFacebook.setOnClickListener(v ->
                openUrl("https://www.facebook.com/yourpage")
        );

        btnInstagram.setOnClickListener(v ->
                openUrl("https://www.instagram.com/yourpage")
        );

        btnTelegram.setOnClickListener(v ->
                openUrl("https://t.me/yourchannel")
        );

        btnYoutube.setOnClickListener(v ->
                openUrl("https://www.youtube.com/@yourchannel")
        );
    }

    /**
     * 🌍 Open browser safely
     */
    private void openUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
}
