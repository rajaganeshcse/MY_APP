package com.app.rewardsplanet.invite;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.rewardsplanet.R;
import com.app.rewardsplanet.UserPref;
import com.app.rewardsplanet.models.UserModel;
import com.app.rewardsplanet.network.ApiClient;
import com.app.rewardsplanet.network.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class layout_invite extends Fragment {

    // ================= UI =================
    private TextView txtCode;
    private EditText edtReferral;
    private View btnCopy, btnValidate, btnShareAll;

    // ================= FIREBASE & BACKEND =================
    private FirebaseFirestore db;
    private String uid;
    private ApiService apiService;

    // ================= LOCAL CACHE =================
    private UserPref userPref;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                com.app.rewardsplanet.R.layout.activity_layout_invite,
                container,
                false
        );
        makeFullScreen();

        // UI
        txtCode = view.findViewById(com.app.rewardsplanet.R.id.txtReferralCode);
        edtReferral = view.findViewById(com.app.rewardsplanet.R.id.edtReferral);
        btnCopy = view.findViewById(com.app.rewardsplanet.R.id.btnCopy);
        btnValidate = view.findViewById(com.app.rewardsplanet.R.id.btnValidate);
        btnShareAll = view.findViewById(R.id.btnShareAll);

        // Firebase & Network
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        apiService = ApiClient.getClient().create(ApiService.class);

        // Local
        userPref = new UserPref(view.getContext());

        if (uid == null) {
            toast("User not logged in");
            return view;
        }

        loadReferralCode();

        if (btnCopy != null) btnCopy.setOnClickListener(v -> copyCode());
        if (btnValidate != null) btnValidate.setOnClickListener(v -> validateReferral());
        if (btnShareAll != null) btnShareAll.setOnClickListener(v -> shareAll());

        return view;
    }

    private void makeFullScreen() {
        if (getActivity() == null) return;
        Window window = getActivity().getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.parseColor("#ffffff"));
        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    // ================= REFERRAL CODE (BACKEND GENERATED & FETCHED) =================

    private void loadReferralCode() {
        if (userPref != null) {
            String localCode = userPref.getReferralCode();
            if (localCode != null && !localCode.trim().isEmpty()) {
                txtCode.setText(localCode);
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            loadReferralCodeFallback();
            return;
        }

        user.getIdToken(false).addOnSuccessListener(tokenResult -> {
            String token = tokenResult.getToken();
            apiService.getReferralCode("Bearer " + token).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String json = response.body().string();
                            JSONObject jsonObject = new JSONObject(json);
                            if (jsonObject.optBoolean("success", false)) {
                                String code = jsonObject.optString("referralCode", "");
                                if (!code.isEmpty()) {
                                    txtCode.setText(code);
                                    if (userPref != null) userPref.setReferralCode(code);
                                    return;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    loadReferralCodeFallback();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    loadReferralCodeFallback();
                }
            });
        }).addOnFailureListener(e -> loadReferralCodeFallback());
    }

    private void loadReferralCodeFallback() {
        if (uid == null) return;
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    UserModel user = doc.toObject(UserModel.class);
                    if (user != null && user.getReferralCode() != null && !user.getReferralCode().isEmpty()) {
                        txtCode.setText(user.getReferralCode());
                        if (userPref != null) userPref.setReferralCode(user.getReferralCode());
                    }
                });
    }

    // ================= COPY =================

    private void copyCode() {
        Context context = getContext();
        if (context == null) return;
        ClipboardManager cm =
                (ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);

        if (cm != null && txtCode != null) {
            cm.setPrimaryClip(
                    ClipData.newPlainText(
                            "referral",
                            txtCode.getText().toString()
                    )
            );
            toast("Code copied");
        }
    }

    // ================= VALIDATE & APPLY REFERRAL (BACKEND SERVER-SIDE VALIDATION) =================

    private void validateReferral() {
        if (edtReferral == null) return;
        String code = edtReferral.getText().toString().trim();

        if (code.isEmpty()) {
            toast("Enter referral code");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            toast("User not logged in");
            return;
        }

        btnValidate.setEnabled(false);

        user.getIdToken(false).addOnSuccessListener(tokenResult -> {
            String token = tokenResult.getToken();
            Map<String, String> body = new HashMap<>();
            body.put("referralCode", code);

            apiService.applyReferralCode("Bearer " + token, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String jsonStr = response.body().string();
                            JSONObject jsonObj = new JSONObject(jsonStr);
                            String msg = jsonObj.optString("message", "Referral applied successfully!");
                            int coinsEarned = jsonObj.optInt("coinsEarned", 250);

                            if (userPref != null) {
                                long currentCoins = userPref.getCoins();
                                userPref.setCoins(currentCoins + coinsEarned);
                            }

                            btnValidate.setEnabled(false);
                            edtReferral.setText("");
                            showRewardResultDialog(coinsEarned, 10, "Referral Bonus Claimed! 🎉");
                        } catch (Exception e) {
                            btnValidate.setEnabled(true);
                            showRewardResultDialog(250, 10, "Referral Bonus Claimed! 🎉");
                        }
                    } else {
                        btnValidate.setEnabled(true);
                        String errMsg = "Failed to apply referral code";
                        try {
                            if (response.errorBody() != null) {
                                String errStr = response.errorBody().string();
                                JSONObject errObj = new JSONObject(errStr);
                                if (errObj.has("message")) {
                                    errMsg = errObj.getString("message");
                                }
                            }
                        } catch (Exception ignored) {}
                        toast(errMsg);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    btnValidate.setEnabled(true);
                    toast("Network error: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> {
            btnValidate.setEnabled(true);
            toast("Authentication failed: " + e.getMessage());
        });
    }

    private void showRewardResultDialog(int coins, int tickets, String titleText) {
        if (getContext() == null || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) return;

        try {
            android.app.Dialog d = new android.app.Dialog(requireContext());
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(R.layout.dialog_spin_result);

            TextView txtTitle = d.findViewById(R.id.txtTitle);
            TextView txtWin = d.findViewById(R.id.txtWinAmount);
            View layoutTicket = d.findViewById(R.id.layoutWinTicket);
            TextView txtTicketAmount = d.findViewById(R.id.txtWinTicketAmount);
            TextView txtBal = d.findViewById(R.id.txtCurrentBalance);
            com.google.android.material.button.MaterialButton ok = d.findViewById(R.id.btnOk);

            if (txtTitle != null) {
                txtTitle.setText(titleText);
            }
            if (txtWin != null) {
                txtWin.setText("+" + coins + " Coins");
            }
            if (tickets > 0) {
                if (layoutTicket != null) layoutTicket.setVisibility(View.VISIBLE);
                if (txtTicketAmount != null) {
                    txtTicketAmount.setText("+" + tickets + " Ticket" + (tickets > 1 ? "s" : ""));
                }
            } else if (layoutTicket != null) {
                layoutTicket.setVisibility(View.GONE);
            }

            if (txtBal != null && userPref != null) {
                txtBal.setText("Balance: " + userPref.getCoins() + " Coins");
            }

            if (ok != null) {
                ok.setText("COLLECT REWARD 🎁");
                ok.setOnClickListener(v -> {
                    try { d.dismiss(); } catch (Exception ignored) {}
                });
            }

            if (d.getWindow() != null) {
                d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            com.app.rewardsplanet.utils.SuccessAnimationHelper.animate(d);
            d.show();

        } catch (Exception e) {
            toast("🎉 +" + coins + " Coins & +" + tickets + " Tickets Added!");
        }
    }

    // ================= SHARE =================

    private String getShareMessage() {
        Context ctx = getContext();
        String pkgName = ctx != null ? ctx.getPackageName() : "com.app.rewardsplanet";
        String code = txtCode != null ? txtCode.getText().toString() : "";
        return "🎮 Join Gamex play & earn FREE coins!\n\n"+
                "🎁 Get ₹20 bonus instantly when you sign up with my link\n\n"+
                "⚡ Play games, complete simple tasks & earn real cash\n\n"+
                "✅ Withdraw easily once your wallet hits just ₹60.0\n\n"
                + "Use my referral code: " + code
                + "\n\nDownload now 👇\n"
                + "🔗 "+"https://play.google.com/store/apps/details?id="
                + pkgName;
    }

    private void shareAll() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getShareMessage());
        startActivity(Intent.createChooser(intent, "Invite using"));
    }

    // ================= TOAST =================

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
