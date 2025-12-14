package com.example.rgamer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;

public class HomeFragment extends Fragment {

    // Header views
    TextView txtCoins, txtToken;
    ImageView imgProfile;

    // Card
    CardView cardInvite;

    UserPref userPref;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        userPref = new UserPref(requireContext());

        // Header
        txtCoins = view.findViewById(R.id.txtCoins);
        txtToken = view.findViewById(R.id.txtToken);
        imgProfile = view.findViewById(R.id.imgProfile);

        // Cards
        cardInvite = view.findViewById(R.id.card_invite);

        loadUserData();

        cardInvite.setOnClickListener(v -> inviteFriend());

        return view;
    }

    private void loadUserData() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(String.valueOf(userPref.getToken()));

        Glide.with(this)
                .load(userPref.getProfileImage())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);
    }

    private void inviteFriend() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.invite_banner);

            File cacheDir = new File(requireContext().getCacheDir(), "images");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File file = new File(cacheDir, "invite.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri imageUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".provider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_TEXT, "Download R Gamer now!");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Invite via"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
