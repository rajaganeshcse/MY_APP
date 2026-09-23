package com.app.rewardsplanet.Activitys;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.rewardsplanet.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private AdView adViewPrivacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        setupStatusBar();

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnOpenWebPolicy = findViewById(R.id.btnOpenWebPolicy);
        if (btnOpenWebPolicy != null) {
            btnOpenWebPolicy.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://yourwebsite.com/privacy-policy"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show();
                }
            });
        }

        adViewPrivacy = findViewById(R.id.adViewPrivacy);
        if (adViewPrivacy != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewPrivacy.loadAd(adRequest);
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adViewPrivacy != null) adViewPrivacy.resume();
    }

    @Override
    protected void onPause() {
        if (adViewPrivacy != null) adViewPrivacy.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (adViewPrivacy != null) adViewPrivacy.destroy();
        super.onDestroy();
    }
}
