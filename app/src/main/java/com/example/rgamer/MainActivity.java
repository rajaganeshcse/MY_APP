package com.example.rgamer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    UserPref userPref;

    TextView txtCoins, txtToken;
    ImageView imgProfile, imgBottomProfile;
    CardView cardInvite;

    LinearLayout navHome, navGame, navReward, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ SAFE STATUS BAR (NO CRASH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        setContentView(R.layout.activity_main);

        userPref = new UserPref(this);

        initViews();
        loadUserData();
        setupClicks();
    }

    private void initViews() {
        txtCoins = findViewById(R.id.txtCoins);
        txtToken = findViewById(R.id.txtToken);
        imgProfile = findViewById(R.id.imgProfile);
        imgBottomProfile = findViewById(R.id.imgBottomProfile);
        cardInvite = findViewById(R.id.card_invite);

        navHome = findViewById(R.id.navHome);
        navGame = findViewById(R.id.navGame);
        navReward = findViewById(R.id.navReward);
        navProfile = findViewById(R.id.navProfile);
    }

    private void loadUserData() {
        txtCoins.setText(String.valueOf(userPref.getCoins()));
        txtToken.setText(userPref.getToken());

        Glide.with(this)
                .load(userPref.getProfileImage())
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(imgProfile);

        if (imgBottomProfile != null) {
            Glide.with(this)
                    .load(userPref.getProfileImage())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imgBottomProfile);
        }
    }

    private void setupClicks() {

        cardInvite.setOnClickListener(v -> inviteFriendWithImage());

        // ❌ DO NOT reopen MainActivity
        navProfile.setOnClickListener(v -> {
            // startActivity(new Intent(this, ProfileActivity.class));
        });
    }

    private void inviteFriendWithImage() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.invite_banner);

            File cacheDir = new File(getCacheDir(), "images");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            File file = new File(cacheDir, "invite.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
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
