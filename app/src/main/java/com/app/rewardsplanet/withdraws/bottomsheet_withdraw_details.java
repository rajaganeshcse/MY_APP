package com.app.rewardsplanet.withdraws;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.app.rewardsplanet.Fragements.RedeemFragment;
import com.app.rewardsplanet.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class bottomsheet_withdraw_details extends BottomSheetDialogFragment {

    /* ================= RESULT KEYS ================= */
    public static final String KEY_RESULT = "withdraw_result";
    public static final String KEY_TYPE = "withdraw_type";

    /* ================= UI ================= */
    private TextView txtTitle, btnSubmit;
    private EditText edtUpi, edtBank, edtAcc, edtIfsc;

    private String type = "";
    private boolean isSubmitting = false;

    /* ================= INSTANCE ================= */
    public static bottomsheet_withdraw_details newInstance(String type) {
        bottomsheet_withdraw_details sheet = new bottomsheet_withdraw_details();
        Bundle b = new Bundle();
        b.putString(KEY_TYPE, type);
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.activity_bottomsheet_withdraw_details,
                container,
                false
        );

        /* ================= GET TYPE ================= */
        if (getArguments() != null) {
            type = getArguments().getString(KEY_TYPE, "");
        }

        /* ================= BIND UI ================= */
        txtTitle = view.findViewById(R.id.txtTitle);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        edtUpi = view.findViewById(R.id.edtUpi);
        edtBank = view.findViewById(R.id.edtBankName);
        edtAcc = view.findViewById(R.id.edtAccount);
        edtIfsc = view.findViewById(R.id.edtIfsc);

        hideAllInputs();

        /* ================= SHOW BASED ON TYPE ================= */
        if (RedeemFragment.UPI.equals(type)) {
            txtTitle.setText("Enter UPI ID");
            edtUpi.setVisibility(View.VISIBLE);
        }
        else if (RedeemFragment.BANK.equals(type)) {
            txtTitle.setText("Enter Bank Details");
            edtBank.setVisibility(View.VISIBLE);
            edtAcc.setVisibility(View.VISIBLE);
            edtIfsc.setVisibility(View.VISIBLE);
        }

        btnSubmit.setOnClickListener(v -> submit());

        return view;
    }

    /* ================= SUBMIT ================= */
    private void submit() {

        if (isSubmitting) return;
        isSubmitting = true;
        btnSubmit.setEnabled(false);

        /* ----------- UPI ----------- */
        if (RedeemFragment.UPI.equals(type)) {

            String upi = edtUpi.getText().toString().trim();

            if (upi.isEmpty()) {
                reset("Enter UPI ID");
                return;
            }

            if (!upi.contains("@")) {
                reset("Invalid UPI ID");
                return;
            }

            sendResult(upi);
            return;
        }

        /* ----------- BANK ----------- */
        if (RedeemFragment.BANK.equals(type)) {

            String bank = edtBank.getText().toString().trim();
            String acc = edtAcc.getText().toString().trim();
            String ifsc = edtIfsc.getText().toString().trim().toUpperCase();

            if (bank.isEmpty() || acc.isEmpty() || ifsc.isEmpty()) {
                reset("Fill all bank details");
                return;
            }

            if (acc.length() < 6) {
                reset("Invalid account number");
                return;
            }

            if (ifsc.length() < 6) {
                reset("Invalid IFSC code");
                return;
            }

            String details = bank + " | " + acc + " | " + ifsc;
            sendResult(details);
        }
    }

    /* ================= SEND RESULT ================= */
    private void sendResult(String result) {

        Bundle bundle = new Bundle();
        bundle.putString(KEY_RESULT, result);

        getParentFragmentManager()
                .setFragmentResult(KEY_RESULT, bundle);

        dismiss();
    }

    /* ================= HELPERS ================= */
    private void hideAllInputs() {
        edtUpi.setVisibility(View.GONE);
        edtBank.setVisibility(View.GONE);
        edtAcc.setVisibility(View.GONE);
        edtIfsc.setVisibility(View.GONE);
    }

    private void reset(String msg) {
        isSubmitting = false;
        btnSubmit.setEnabled(true);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
