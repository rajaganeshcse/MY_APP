package com.app.rewardsplanet.lucky_draw;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.ads.AdsManager;
import com.app.rewardsplanet.models.JoinResponse;
import com.app.rewardsplanet.models.LuckyDrawModel;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.util.*;

import retrofit2.*;

public class activity_lucky_draw extends AppCompatActivity
        implements LuckyDrawAdapter.Listener {

    FirebaseFirestore db;
    ApiService api;

    TextView tickets;
    MaterialCardView cardLuckyDrawHistory;

    List<LuckyDrawModel> list = new ArrayList<>();
    LuckyDrawAdapter adapter;

    UserPref userPref;
    String uid;
    int userTickets = 0;

    private RewardedAd rewardedAd;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lucky_draw);

        userPref = new UserPref(this);

        tickets = findViewById(R.id.tickets);

        cardLuckyDrawHistory = findViewById(R.id.cardLuckyDrawHistory);
        if (cardLuckyDrawHistory != null) {
            cardLuckyDrawHistory.setOnClickListener(v ->
                    startActivity(new Intent(this, activity_lucky_draw_winner.class))
            );
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        db = FirebaseFirestore.getInstance();
        api = ApiClient.getClient().create(ApiService.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = (user != null && user.getUid() != null && !user.getUid().isEmpty())
                ? user.getUid()
                : userPref.getUid();

        userTickets = userPref.getTickets();
        if (tickets != null) {
            tickets.setText(String.valueOf(userTickets));
        }

        adapter = new LuckyDrawAdapter(list, this, userTickets);

        RecyclerView rv = findViewById(R.id.luckyDrawRecycler);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(adapter);
        }

        try {
            MobileAds.initialize(this, status -> {});
            loadAd();
        } catch (Exception ignored) {}

        loadDraws();
        makeFullScreen();

        // user tickets listener
        if (uid != null && !uid.isEmpty()) {
            db.collection("users")
                    .document(uid)
                    .addSnapshotListener(this, (snap, e) -> {
                        if (snap != null && snap.exists()) {
                            userTickets = parseTickets(snap);
                            userPref.setTickets(userTickets);
                            if (adapter != null) {
                                adapter.updateUserTickets(userTickets);
                            }
                            if (tickets != null) {
                                tickets.setText(String.valueOf(userTickets));
                            }
                        }
                    });
        }
    }

    private int parseTickets(DocumentSnapshot snap) {
        if (snap == null || !snap.exists()) return 0;
        Object val = snap.get("tickets");
        if (val == null) val = snap.get("ticket");
        if (val == null) val = snap.get("user_tickets");
        if (val == null) val = snap.get("total_tickets");
        if (val == null) val = snap.get("tokens");

        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val instanceof String) {
            try {
                return Integer.parseInt(((String) val).trim());
            } catch (Exception ignored) {}
        }
        return 0;
    }

    /* ================= FULL SCREEN ================= */

    private void makeFullScreen() {
        Window window = getWindow();

        // 🔥 Make content go behind system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );

                // Optional: hide bars (remove if you only want transparent top)
                // controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }

        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        // 🔥 Make status bar transparent (TOP FIX)
        window.setStatusBarColor(Color.TRANSPARENT);

        // 🔥 Optional: make navigation bar transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    /* ================= LOAD DRAWS ================= */

    private void loadDraws() {
        db.collection("lucky_draws")
                .whereEqualTo("status", "OPEN")
                .addSnapshotListener(this, (snap, e) -> {

                    if (snap == null) return;

                    list.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        LuckyDrawModel m = d.toObject(LuckyDrawModel.class);
                        if (m == null) continue;

                        m.setId(d.getId());

                        // Fallback manual field population for slots and limits
                        Long fSlots = d.getLong("filledSlots");
                        if (fSlots == null) fSlots = d.getLong("currentParticipation");
                        if (fSlots != null) m.setFilledSlots(fSlots);

                        Long tSlots = d.getLong("totalSlots");
                        if (tSlots == null) tSlots = d.getLong("participationLimit");
                        if (tSlots != null) m.setTotalSlots(tSlots);

                        Long rCoins = d.getLong("rewardCoins");
                        if (rCoins != null) m.setRewardCoins(rCoins);

                        Long tCost = d.getLong("ticketCost");
                        if (tCost != null) m.setTicketCost(tCost);

                        checkUserEntries(m);
                        list.add(m);
                    }

                    // Sort ascending by reward coins (e.g. 25 -> 100 -> 200 -> 250 -> 500 -> 1000)
                    try {
                        Collections.sort(list, (a, b) -> Integer.compare(a.getRewardCoins(), b.getRewardCoins()));
                    } catch (Exception ignored) {}

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /* ================= CHECK USER ENTRIES ================= */

    private void checkUserEntries(LuckyDrawModel model) {

        if (uid == null || uid.isEmpty() || model == null || model.getId() == null || model.getId().isEmpty()) return;

        db.collection("lucky_draw_tickets")
                .document(model.getId())
                .collection("tickets")
                .get()
                .addOnSuccessListener(snap -> {

                    int ticketCount = 0;
                    boolean adUsed = false;

                    if (snap != null) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String ticketUid = d.getString("uid");
                            if (ticketUid == null) ticketUid = d.getString("userId");

                            if (uid.equals(ticketUid)) {
                                String type = d.getString("type");

                                if ("AD".equalsIgnoreCase(type)) adUsed = true;
                                else ticketCount++;
                            }
                        }
                    }

                    model.setAdJoined(adUsed);
                    model.setMyTicketsCount(ticketCount);

                    if (adapter != null) {
                        adapter.clearLoading(model.getId());
                    }
                    if (list.contains(model)) {
                        try {
                            Collections.sort(list, (a, b) -> Integer.compare(a.getRewardCoins(), b.getRewardCoins()));
                        } catch (Exception ignored) {}
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (adapter != null) {
                        adapter.clearLoading(model.getId());
                    }
                });
    }

    /* ================= AD ================= */

    private void loadAd() {
        try {
            AdRequest adRequest = new AdRequest.Builder().build();

            RewardedAd.load(this,
                    AdsManager.REWARDED_AD_ID,
                    adRequest,
                    new RewardedAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedAd = ad;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            rewardedAd = null;
                        }
                    });
        } catch (Exception e) {
            rewardedAd = null;
        }
    }

    /* ================= CLICK ================= */

    @Override
    public void onJoin(LuckyDrawModel model) {
        showConfirmDialog(model, "AD");
    }

    @Override
    public void onJoinWithTickets(LuckyDrawModel model) {
        showConfirmDialog(model, "TICKET");
    }

    @Override
    public void onCheckWinners(LuckyDrawModel model) {
        Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewMyTokens(LuckyDrawModel model) {
        if (model == null || uid == null || uid.isEmpty()) return;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_my_tokens, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        LinearLayout layoutTokensList = view.findViewById(R.id.layoutTokensList);
        TextView txtContestInfo = view.findViewById(R.id.txtContestInfo);
        TextView txtTotalJoinedBadge = view.findViewById(R.id.txtTotalJoinedBadge);
        TextView txtTokensList = view.findViewById(R.id.txtTokensList);
        ImageView btnClose = view.findViewById(R.id.btnClose);
        MaterialButton btnGotIt = view.findViewById(R.id.btnGotIt);
        MaterialButton btnCopyTokens = view.findViewById(R.id.btnCopyTokens);

        if (txtContestInfo != null) {
            txtContestInfo.setText("Win " + model.getRewardCoins() + " Coins Contest");
        }

        int totalJoined = model.getMyTicketsCount() + (model.isAdJoined() ? 1 : 0);
        if (txtTotalJoinedBadge != null) {
            txtTotalJoinedBadge.setText("✓ " + totalJoined + " Ticket" + (totalJoined > 1 ? "s" : "") + " Submitted");
        }

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnGotIt != null) btnGotIt.setOnClickListener(v -> dialog.dismiss());

        final List<String> fetchedTokens = new ArrayList<>();

        db.collection("lucky_draw_tickets")
                .document(model.getId())
                .collection("tickets")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String ticketUid = d.getString("uid");
                            if (ticketUid == null) ticketUid = d.getString("userId");

                            if (uid.equals(ticketUid)) {
                                String tokenStr = d.getString("token");
                                if (tokenStr != null && !tokenStr.isEmpty()) {
                                    fetchedTokens.add(tokenStr);
                                }
                            }
                        }
                    }

                    // Fallback to myTickets collection if lucky_draw_tickets returned no matches
                    if (fetchedTokens.isEmpty()) {
                        db.collection("users")
                                .document(uid)
                                .collection("myTickets")
                                .whereEqualTo("drawId", model.getId())
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    if (userSnap != null && !userSnap.isEmpty()) {
                                        for (DocumentSnapshot d : userSnap.getDocuments()) {
                                            String tokenStr = d.getString("token");
                                            if (tokenStr != null && !tokenStr.isEmpty()) {
                                                fetchedTokens.add(tokenStr);
                                            }
                                        }
                                    }

                                    renderFetchedTokens(layoutTokensList, txtTokensList, fetchedTokens);
                                })
                                .addOnFailureListener(e -> renderFetchedTokens(layoutTokensList, txtTokensList, fetchedTokens));
                    } else {
                        renderFetchedTokens(layoutTokensList, txtTokensList, fetchedTokens);
                    }
                })
                .addOnFailureListener(e -> {
                    renderFetchedTokens(layoutTokensList, txtTokensList, fetchedTokens);
                });

        if (btnCopyTokens != null) {
            btnCopyTokens.setOnClickListener(v -> {
                if (!fetchedTokens.isEmpty()) {
                    StringBuilder sbCopy = new StringBuilder();
                    for (int i = 0; i < fetchedTokens.size(); i++) {
                        sbCopy.append(fetchedTokens.get(i));
                        if (i < fetchedTokens.size() - 1) sbCopy.append("\n");
                    }
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("My Ticket Tokens", sbCopy.toString());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "Tokens copied to clipboard!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No tokens to copy", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void renderFetchedTokens(LinearLayout layoutTokensList, TextView txtTokensList, List<String> fetchedTokens) {
        if (layoutTokensList != null && !fetchedTokens.isEmpty()) {
            com.app.rewardsplanet.utils.TokenBlockHelper.renderTokenBlocks(layoutTokensList, fetchedTokens);
        } else if (txtTokensList != null) {
            if (!fetchedTokens.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    txtTokensList.setText(String.join("\n", fetchedTokens));
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < fetchedTokens.size(); i++) {
                        sb.append(fetchedTokens.get(i));
                        if (i < fetchedTokens.size() - 1) sb.append("\n");
                    }
                    txtTokensList.setText(sb.toString());
                }
            } else {
                txtTokensList.setText("No tokens found for this contest.");
            }
        }
    }

    private int selectedTicketQty = 1;

    /* ================= CONFIRM DIALOG ================= */

    private void showConfirmDialog(LuckyDrawModel model, String type) {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirmation, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        TextView txtMessage = view.findViewById(R.id.txtMessage);
        TextView txtSubtitle = view.findViewById(R.id.Message);
        View layoutQuantity = view.findViewById(R.id.layoutQuantityContainer);
        View btnMinus = view.findViewById(R.id.btnMinus);
        View btnPlus = view.findViewById(R.id.btnPlus);
        View btnMax = view.findViewById(R.id.btnMax);
        TextView txtQuantity = view.findViewById(R.id.txtQuantity);
        TextView txtTicketBalance = view.findViewById(R.id.txtTicketBalance);

        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        ImageView closebtn = view.findViewById(R.id.btnClose);

        final int costPerEntry = Math.max(1, model.getTicketCost());
        final int remainingSlots = Math.max(0, model.getTotalSlots() - model.getFilledSlots());
        final int maxEntriesByTickets = userTickets / costPerEntry;
        final int maxAllowed = Math.min(maxEntriesByTickets, remainingSlots);

        selectedTicketQty = 1;

        if ("AD".equals(type)) {
            if (txtTitle != null) txtTitle.setText("Watch Ad 📺");
            if (txtMessage != null) txtMessage.setText("Watch Free Ad Entry");
            if (txtSubtitle != null) txtSubtitle.setText("Watch a short video ad to claim 1 free entry ticket into this draw!");
            if (layoutQuantity != null) layoutQuantity.setVisibility(View.GONE);
            btnConfirm.setText("Watch Ad & Join");
        } else {
            if (txtTitle != null) txtTitle.setText("Ticket Entry 🎟️");
            if (txtMessage != null) txtMessage.setText("Join Contest with Tickets");
            if (txtSubtitle != null) txtSubtitle.setText("1 Entry = " + costPerEntry + " Ticket" + (costPerEntry > 1 ? "s" : ""));
            if (layoutQuantity != null) layoutQuantity.setVisibility(View.VISIBLE);

            if (maxAllowed < 1) {
                selectedTicketQty = 0;
                if (txtQuantity != null) txtQuantity.setText("0");
                if (txtTicketBalance != null) {
                    if (userTickets < costPerEntry) {
                        txtTicketBalance.setText("⚠️ Requires " + costPerEntry + " ticket(s) (You have " + userTickets + ").");
                    } else {
                        txtTicketBalance.setText("⚠️ Contest slots are full.");
                    }
                    txtTicketBalance.setTextColor(Color.parseColor("#EF4444"));
                }
                btnConfirm.setEnabled(false);
                btnConfirm.setText("INSUFFICIENT TICKETS");
            } else {
                selectedTicketQty = 1;
                int totalCost = selectedTicketQty * costPerEntry;
                if (txtQuantity != null) txtQuantity.setText(String.valueOf(selectedTicketQty));
                if (txtTicketBalance != null) {
                    txtTicketBalance.setText("Available: " + userTickets + " Tickets  •  " + remainingSlots + " Slots Left");
                    txtTicketBalance.setTextColor(Color.parseColor("#64748B"));
                }
                btnConfirm.setEnabled(true);
                btnConfirm.setText("JOIN DRAW · " + totalCost + " Ticket" + (totalCost > 1 ? "s" : ""));
            }

            if (btnMinus != null) {
                btnMinus.setOnClickListener(v -> {
                    if (selectedTicketQty > 1) {
                        selectedTicketQty--;
                        int totalCost = selectedTicketQty * costPerEntry;
                        if (txtQuantity != null) txtQuantity.setText(String.valueOf(selectedTicketQty));
                        btnConfirm.setText("JOIN DRAW · " + totalCost + " Ticket" + (totalCost > 1 ? "s" : ""));
                    }
                });
            }

            if (btnPlus != null) {
                btnPlus.setOnClickListener(v -> {
                    if (selectedTicketQty < maxAllowed) {
                        selectedTicketQty++;
                        int totalCost = selectedTicketQty * costPerEntry;
                        if (txtQuantity != null) txtQuantity.setText(String.valueOf(selectedTicketQty));
                        btnConfirm.setText("JOIN DRAW · " + totalCost + " Ticket" + (totalCost > 1 ? "s" : ""));
                    } else if (maxAllowed > 0 && selectedTicketQty >= maxAllowed) {
                        Toast.makeText(this, "Max entry limit reached (" + maxAllowed + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (btnMax != null) {
                btnMax.setOnClickListener(v -> {
                    if (maxAllowed > 0) {
                        selectedTicketQty = maxAllowed;
                        int totalCost = selectedTicketQty * costPerEntry;
                        if (txtQuantity != null) txtQuantity.setText(String.valueOf(selectedTicketQty));
                        btnConfirm.setText("JOIN DRAW · " + totalCost + " Ticket" + (totalCost > 1 ? "s" : ""));
                    }
                });
            }
        }

        if (closebtn != null) {
            closebtn.setOnClickListener(v -> {
                if (adapter != null) adapter.clearLoading(model.getId());
                try { dialog.dismiss(); } catch (Exception ignored) {}
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (adapter != null) adapter.clearLoading(model.getId());
                try { dialog.dismiss(); } catch (Exception ignored) {}
            });
        }

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            try { dialog.dismiss(); } catch (Exception ignored) {}

            if ("AD".equals(type)) {
                showAdThenJoin(model);
            } else {
                showLoading();
                join(model.getId(), type, selectedTicketQty);
            }
        });
    }

    /* ================= LOADING ================= */

    private void showLoading() {
        if (isFinishing() || isDestroyed()) return;
        try {
            hideLoading();
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);

            loadingDialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create();

            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            loadingDialog.show();
        } catch (Exception ignored) {}
    }

    private void hideLoading() {
        if (loadingDialog != null) {
            try {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            } catch (Exception ignored) {}
            loadingDialog = null;
        }
    }

    /* ================= AD FLOW ================= */

    private void showAdThenJoin(LuckyDrawModel model) {

        if (rewardedAd != null) {

            if (adapter != null) adapter.setLoading(model.getId(), true);

            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdDismissedFullScreenContent() {
                    if (adapter != null) adapter.clearLoading(model.getId());
                    rewardedAd = null;
                    loadAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    if (adapter != null) adapter.clearLoading(model.getId());
                }
            });

            rewardedAd.show(this, rewardItem -> {
                showLoading();
                join(model.getId(), "AD", 1);
            });

        } else {
            Toast.makeText(this, "Ad not ready", Toast.LENGTH_SHORT).show();
            if (adapter != null) adapter.clearLoading(model.getId());
            loadAd();
        }
    }

    /* ================= API ================= */

    private void join(String drawId, String type, int count) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            hideLoading();
            if (adapter != null) adapter.clearLoading(drawId);
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        user.getIdToken(false)
                .addOnSuccessListener(result -> {

                    String token = result.getToken();

                    Map<String, Object> body = new HashMap<>();
                    body.put("drawId", drawId);
                    body.put("type", type);
                    body.put("count", count);

                    api.joinDraw("Bearer " + token, body)
                            .enqueue(new Callback<JoinResponse>() {

                                @Override
                                public void onResponse(Call<JoinResponse> call,
                                                       Response<JoinResponse> response) {

                                    hideLoading();
                                    if (adapter != null) adapter.clearLoading(drawId);

                                    if (isFinishing() || isDestroyed()) return;

                                    if (response.isSuccessful() && response.body() != null) {

                                        JoinResponse res = response.body();
                                        showSuccessDialog(res.message, res.tokens);

                                        for (LuckyDrawModel m : list) {
                                            if (m.getId().equals(drawId)) {

                                                if ("AD".equals(type)) {
                                                    m.setAdJoined(true);
                                                } else {
                                                    m.setMyTicketsCount(
                                                            m.getMyTicketsCount() + count
                                                    );
                                                }
                                                m.setFilledSlots((long) (m.getFilledSlots() + count));
                                                break;
                                            }
                                        }

                                        try {
                                            Collections.sort(list, (a, b) -> Integer.compare(a.getRewardCoins(), b.getRewardCoins()));
                                            if (adapter != null) adapter.notifyDataSetChanged();
                                        } catch (Exception ignored) {}

                                        if ("TICKET".equals(type)) {
                                            if (res.remainingTickets != null && res.remainingTickets >= 0) {
                                                userTickets = res.remainingTickets;
                                            } else {
                                                int deducted = (res.ticketsDeducted != null && res.ticketsDeducted > 0)
                                                        ? res.ticketsDeducted
                                                        : count;
                                                userTickets = Math.max(0, userTickets - deducted);
                                            }
                                            userPref.setTickets(userTickets);
                                            if (adapter != null) adapter.updateUserTickets(userTickets);
                                            if (tickets != null) {
                                                tickets.setText(String.valueOf(userTickets));
                                            }
                                        }

                                    } else {
                                        String errMsg = "Join failed";
                                        try {
                                            if (response.errorBody() != null) {
                                                String errStr = response.errorBody().string();
                                                if (errStr.contains("message")) {
                                                    org.json.JSONObject obj = new org.json.JSONObject(errStr);
                                                    if (obj.has("message")) errMsg = obj.getString("message");
                                                }
                                            }
                                        } catch (Exception ignored) {}

                                        Toast.makeText(activity_lucky_draw.this,
                                                errMsg, Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<JoinResponse> call, Throwable t) {
                                    hideLoading();
                                    if (adapter != null) adapter.clearLoading(drawId);
                                    if (isFinishing() || isDestroyed()) return;
                                    Toast.makeText(activity_lucky_draw.this,
                                            t.getMessage() != null ? t.getMessage() : "Network error", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    if (adapter != null) adapter.clearLoading(drawId);
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(activity_lucky_draw.this,
                            "Authentication error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /* ================= SUCCESS ================= */

    private void showSuccessDialog(String msg, List<String> tokens) {
        if (isFinishing() || isDestroyed()) return;

        try {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_draw_success, null);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dialog.show();
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);

            com.app.rewardsplanet.utils.SuccessAnimationHelper.animate(dialog);

            LinearLayout layoutTokensList = view.findViewById(R.id.layoutTokensList);
            TextView txtMsg = view.findViewById(R.id.txtSuccessMsg);
            TextView txtTokensList = view.findViewById(R.id.txtTokensList);
            View cardTokensContainer = view.findViewById(R.id.cardTokensContainer);
            MaterialButton btnAwesome = view.findViewById(R.id.btnAwesome);

            if (txtMsg != null) {
                txtMsg.setText(msg != null ? msg : "Entry Success!");
            }

            if (tokens != null && !tokens.isEmpty()) {
                if (cardTokensContainer != null) cardTokensContainer.setVisibility(View.VISIBLE);
                if (layoutTokensList != null) {
                    com.app.rewardsplanet.utils.TokenBlockHelper.renderTokenBlocks(layoutTokensList, tokens);
                } else if (txtTokensList != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        txtTokensList.setText(String.join("\n", tokens));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < tokens.size(); i++) {
                            sb.append(tokens.get(i));
                            if (i < tokens.size() - 1) sb.append("\n");
                        }
                        txtTokensList.setText(sb.toString());
                    }
                }
            } else {
                if (cardTokensContainer != null) cardTokensContainer.setVisibility(View.GONE);
            }

            if (btnAwesome != null) {
                btnAwesome.setOnClickListener(v -> {
                    try { dialog.dismiss(); } catch (Exception ignored) {}
                });
            }
        } catch (Exception e) {
            Toast.makeText(this, msg != null ? msg : "Joined successfully!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoading();
    }
}