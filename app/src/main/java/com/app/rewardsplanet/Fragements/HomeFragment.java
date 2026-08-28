package com.app.rewardsplanet.Fragements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.R;

public class HomeFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private ImageView menuIcon;

    private LinearLayout menuHome;
    private LinearLayout menuCategories;
    private LinearLayout menuStores;

    private LinearLayout menuWallet;
    private LinearLayout menuActivity;
    private LinearLayout menuRefer;

    private LinearLayout menuRate;
    private LinearLayout menuFeedback;
    private LinearLayout menuContact;
    private LinearLayout menuFaq;
    private LinearLayout menuPrivacy;


    public HomeFragment() {
        // Required empty constructor
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );
    }


    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);


        // =====================================================
        // DRAWER
        // =====================================================

        drawerLayout = view.findViewById(R.id.drawerLayout);

        if (drawerLayout == null) {
            return;
        }


        // =====================================================
        // MENU ICON
        // =====================================================

        menuIcon = view.findViewById(R.id.menuIcon);

        if (menuIcon != null) {

            menuIcon.setOnClickListener(v -> {

                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

                    drawerLayout.closeDrawer(
                            GravityCompat.START
                    );

                } else {

                    drawerLayout.openDrawer(
                            GravityCompat.START
                    );
                }

            });
        }


        // =====================================================
        // MENU ITEMS
        // =====================================================

        menuHome = view.findViewById(R.id.menuHome);
        menuCategories = view.findViewById(R.id.menuCategories);
        menuStores = view.findViewById(R.id.menuStores);

        menuWallet = view.findViewById(R.id.menuWallet);
        menuActivity = view.findViewById(R.id.menuActivity);
        menuRefer = view.findViewById(R.id.menuRefer);

        menuRate = view.findViewById(R.id.menuRate);
        menuFeedback = view.findViewById(R.id.menuFeedback);
        menuContact = view.findViewById(R.id.menuContact);
        menuFaq = view.findViewById(R.id.menuFaq);
        menuPrivacy = view.findViewById(R.id.menuPrivacy);


        // =====================================================
        // HOME
        // =====================================================

        if (menuHome != null) {
            menuHome.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Home",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // ALL CATEGORIES
        // =====================================================

        if (menuCategories != null) {
            menuCategories.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "All Categories",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // ALL STORES
        // =====================================================

        if (menuStores != null) {
            menuStores.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "All Stores",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // WALLET
        // =====================================================

        if (menuWallet != null) {
            menuWallet.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Wallet",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // MY ACTIVITY
        // =====================================================

        if (menuActivity != null) {
            menuActivity.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "My Activity",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // REFER AND EARN
        // =====================================================

        if (menuRefer != null) {
            menuRefer.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Refer and Earn",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // RATE US
        // =====================================================

        if (menuRate != null) {
            menuRate.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Rate Us",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // FEEDBACK
        // =====================================================

        if (menuFeedback != null) {
            menuFeedback.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Feedback",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // CONTACT US
        // =====================================================

        if (menuContact != null) {
            menuContact.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Contact Us",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // FAQ
        // =====================================================

        if (menuFaq != null) {
            menuFaq.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "FAQ's",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }


        // =====================================================
        // PRIVACY POLICY
        // =====================================================

        if (menuPrivacy != null) {
            menuPrivacy.setOnClickListener(v -> {

                closeDrawer();

                Toast.makeText(
                        requireContext(),
                        "Privacy Policy",
                        Toast.LENGTH_SHORT
                ).show();

            });
        }
    }


    // =========================================================
    // CLOSE DRAWER
    // =========================================================

    private void closeDrawer() {

        if (drawerLayout != null &&
                drawerLayout.isDrawerOpen(GravityCompat.START)) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }
    }


    // =========================================================
    // DESTROY VIEW
    // =========================================================

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        drawerLayout = null;
        menuIcon = null;

        menuHome = null;
        menuCategories = null;
        menuStores = null;

        menuWallet = null;
        menuActivity = null;
        menuRefer = null;

        menuRate = null;
        menuFeedback = null;
        menuContact = null;
        menuFaq = null;
        menuPrivacy = null;
    }
}