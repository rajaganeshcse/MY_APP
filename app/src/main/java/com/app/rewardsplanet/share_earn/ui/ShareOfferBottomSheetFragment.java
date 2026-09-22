package com.app.rewardsplanet.share_earn.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.share_earn.model.ShareEarnOffer;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ShareOfferBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_OFFER = "arg_offer";
    private static final String ARG_TRACKING_URL = "arg_tracking_url";

    private ShareEarnOffer offer;
    private String trackingUrl;

    public static ShareOfferBottomSheetFragment newInstance(ShareEarnOffer offer, String trackingUrl) {
        ShareOfferBottomSheetFragment fragment = new ShareOfferBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_OFFER, offer);
        args.putString(ARG_TRACKING_URL, trackingUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            offer = (ShareEarnOffer) getArguments().getSerializable(ARG_OFFER);
            trackingUrl = getArguments().getString(ARG_TRACKING_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_share_offer, container, false);

        ImageView imgLogo = v.findViewById(R.id.imgSheetLogo);
        TextView txtReward = v.findViewById(R.id.txtSheetReward);
        TextView txtSubtitle = v.findViewById(R.id.txtSheetSubtitle);
        TextView txtTrackingUrl = v.findViewById(R.id.txtTrackingUrl);
        ImageView btnCopyLink = v.findViewById(R.id.btnCopyLink);

        View btnWhatsApp = v.findViewById(R.id.btnShareWhatsApp);
        View btnTelegram = v.findViewById(R.id.btnShareTelegram);
        View btnMessages = v.findViewById(R.id.btnShareMessages);
        View btnMore = v.findViewById(R.id.btnShareMore);
        Button btnCancel = v.findViewById(R.id.btnCancelSheet);

        if (offer != null) {
            txtReward.setText("earn " + String.format("%,d", offer.getRewardCoins()) + " Coins");
            txtSubtitle.setText(offer.getShortDescription() != null ? offer.getShortDescription() : offer.getTitle());

            if (offer.getLogoUrl() != null && !offer.getLogoUrl().trim().isEmpty()) {
                Glide.with(requireContext()).load(offer.getLogoUrl()).placeholder(R.drawable.logo).error(R.drawable.logo).into(imgLogo);
            }
        }

        txtTrackingUrl.setText(trackingUrl != null ? trackingUrl : "");

        btnCopyLink.setOnClickListener(view -> copyToClipboard());
        txtTrackingUrl.setOnClickListener(view -> copyToClipboard());

        Button btnOpenBrowser = v.findViewById(R.id.btnOpenBrowser);
        if (btnOpenBrowser != null) {
            btnOpenBrowser.setOnClickListener(view -> {
                if (trackingUrl != null && !trackingUrl.isEmpty()) {
                    try {
                        Intent intent = new Intent(requireContext(), RedirectCountdownActivity.class);
                        intent.putExtra(RedirectCountdownActivity.EXTRA_REDIRECT_URL, trackingUrl);
                        if (offer != null) {
                            intent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_TITLE, offer.getTitle());
                            intent.putExtra(RedirectCountdownActivity.EXTRA_OFFER_LOGO, offer.getLogoUrl());
                            intent.putExtra(RedirectCountdownActivity.EXTRA_REWARD_COINS, offer.getRewardCoins());
                        }
                        startActivity(intent);
                        dismiss();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Could not open countdown: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        String shareText = getShareMessage();

        btnWhatsApp.setOnClickListener(view -> shareToPackage("com.whatsapp", shareText));
        btnTelegram.setOnClickListener(view -> shareToPackage("org.telegram.messenger", shareText));
        btnMessages.setOnClickListener(view -> launchGenericShare(shareText));
        btnMore.setOnClickListener(view -> launchGenericShare(shareText));

        btnCancel.setOnClickListener(view -> dismiss());

        return v;
    }

    private String getShareMessage() {
        String title = offer != null ? offer.getTitle() : "Check out this offer!";
        long coins = offer != null ? offer.getRewardCoins() : 0;
        return "Check out " + title + "!\n\nComplete the required steps and earn " + String.format("%,d", coins) + " coins.\n\n" + trackingUrl;
    }

    private void copyToClipboard() {
        if (trackingUrl == null) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Share Tracking Link", trackingUrl);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Tracking link copied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareToPackage(String packageName, String text) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setPackage(packageName);
            startActivity(intent);
        } catch (Exception e) {
            launchGenericShare(text);
        }
    }

    private void launchGenericShare(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share via"));
    }
}
