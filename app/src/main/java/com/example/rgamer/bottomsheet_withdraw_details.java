package com.example.rgamer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class bottomsheet_withdraw_details extends BottomSheetDialogFragment {

    // 🔹 Fragment Result Keys
    public static final String KEY_RESULT = "withdraw_result";
    public static final String KEY_TYPE = "withdraw_type";

    // 🔹 UI
    private EditText edtUpi, edtBank, edtAcc, edtIfsc;
    private TextView btnSubmit, txtTitle;

    private String type;

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
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.activity_bottomsheet_withdraw_details,
                container,
                false
        );

        // 🔹 GET TYPE
        type = getArguments() != null
                ? getArguments().getString(KEY_TYPE)
                : "";

        // 🔹 BIND UI
        txtTitle = view.findViewById(R.id.txtTitle);
        edtUpi = view.findViewById(R.id.edtUpi);
        edtBank = view.findViewById(R.id.edtBankName);
        edtAcc = view.findViewById(R.id.edtAccount);
        edtIfsc = view.findViewById(R.id.edtIfsc);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        hideAllInputs();

        // 🔹 SHOW BASED ON TYPE
        if (RedeemFragment.UPI.equals(type)) {
            txtTitle.setText("Enter UPI ID");
            edtUpi.setVisibility(View.VISIBLE);
        }

        if (RedeemFragment.BANK.equals(type)) {
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

        // 🔹 UPI FLOW
        if (RedeemFragment.UPI.equals(type)) {

            String upi = edtUpi.getText().toString().trim();

            if (upi.isEmpty() || !upi.contains("@")) {
                toast("Enter valid UPI ID");
                return;
            }

            sendResult(upi);
        }

        // 🔹 BANK FLOW
        if (RedeemFragment.BANK.equals(type)) {

            String bank = edtBank.getText().toString().trim();
            String acc = edtAcc.getText().toString().trim();
            String ifsc = edtIfsc.getText().toString().trim();

            if (bank.isEmpty() || acc.isEmpty() || ifsc.isEmpty()) {
                toast("Fill all bank details");
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

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
