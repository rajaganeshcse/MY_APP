package com.example.rgamer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    LinearLayout dotsLayout;
    ImageView nextBtn;
    TextView skipBtn;

    SliderAdapter adapter;
    List<SliderModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        nextBtn = findViewById(R.id.nextBtn);
        skipBtn = findViewById(R.id.skipBtn);

        list = new ArrayList<>();
        list.add(new SliderModel(R.drawable.img_redeem, "Redeem Code", "Earn Free Redeem Code"));
        list.add(new SliderModel(R.drawable.img_money, "Earn Money", "Get Real Money & Gift Cards"));
        list.add(new SliderModel(R.drawable.img_game, "Game Credits", "Earn Free Diamonds, Coins, Cash, UC"));

        adapter = new SliderAdapter(this, list);
        viewPager.setAdapter(adapter);

        addDots(0);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                addDots(position);
                if (position == list.size() - 1) nextBtn.setImageResource(R.drawable.done_icon);
                else nextBtn.setImageResource(R.drawable.next_arrow);
            }
        });

        nextBtn.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < list.size() - 1) viewPager.setCurrentItem(pos + 1);
            else finishOnboarding();
        });

        skipBtn.setOnClickListener(v -> finishOnboarding());
    }

    private void addDots(int position) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < list.size(); i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(40, 40);
            params.setMargins(10, 0, 10, 0);
            dotsLayout.addView(dot, params);
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("onboard", MODE_PRIVATE);
        prefs.edit().putBoolean("firstTime", false).apply();
        startActivity(new Intent(this,activity_login.class));
        finish();
    }
}
