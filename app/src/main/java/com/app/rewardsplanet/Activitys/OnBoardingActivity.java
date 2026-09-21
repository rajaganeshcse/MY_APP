package com.app.rewardsplanet.Activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.SliderAdapter;
import com.app.rewardsplanet.models.SliderModel;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private ImageView nextBtn;
    private TextView skipBtn;

    private SliderAdapter adapter;
    private List<SliderModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
        makeFullScreen();

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        nextBtn = findViewById(R.id.nextBtn);
        skipBtn = findViewById(R.id.skipBtn);

        list = new ArrayList<>();
        list.add(new SliderModel(R.drawable.img_redeem, "Redeem Code", "Earn Free Gift Cards & Google Play Redeem Codes"));
        list.add(new SliderModel(R.drawable.img_money, "Earn Cash & Money", "Get Real Instant Cash Rewards directly into your Wallet"));
        list.add(new SliderModel(R.drawable.img_game, "Game Credits & UC", "Earn Free Diamonds, Coins, Cash, and Game UC"));

        adapter = new SliderAdapter(this, list);
        viewPager.setAdapter(adapter);

        // Smooth Page Depth & Scale Transformer
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            if (position < -1 || position > 1) {
                page.setAlpha(0f);
            } else {
                page.setAlpha(1.0f - absPos * 0.4f);
                float scale = 0.85f + (1.0f - absPos) * 0.15f;
                page.setScaleX(scale);
                page.setScaleY(scale);
            }
        });

        addDots(0);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDots(position);
                if (position == list.size() - 1) {
                    nextBtn.setImageResource(R.drawable.done_icon);
                } else {
                    nextBtn.setImageResource(R.drawable.next_arrow);
                }
            }
        });

        // Spring touch feedback on Next Action Button
        nextBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160)
                            .setInterpolator(new OvershootInterpolator(2.2f)).start();
                    break;
            }
            return false;
        });

        nextBtn.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < list.size() - 1) {
                viewPager.setCurrentItem(pos + 1, true);
            } else {
                finishOnboarding();
            }
        });

        skipBtn.setOnClickListener(v -> finishOnboarding());
    }

    private void addDots(int position) {
        if (dotsLayout == null) return;
        dotsLayout.removeAllViews();
        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);
        int dp24 = (int) (24 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < list.size(); i++) {
            View dot = new View(this);
            int width = (i == position) ? dp24 : dp8;
            int height = dp8;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(margin, 0, margin, 0);

            dot.setBackgroundResource(i == position ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive);
            dotsLayout.addView(dot, params);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("onboard", MODE_PRIVATE);
        prefs.edit().putBoolean("firstTime", false).apply();
        Intent intent = new Intent(this, activity_login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void makeFullScreen() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }
}
