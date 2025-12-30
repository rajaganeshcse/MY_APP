package com.example.rgamer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class JoinGameIdBottomSheet extends BottomSheetDialogFragment {

    public interface Callback {
        void onConfirm(String gameId);
    }

    private final String game;
    private final Callback callback;

    public JoinGameIdBottomSheet(String game, Callback callback) {
        this.game = game;
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(
                R.layout.activity_join_game_id_bottom_sheet,
                container,
                false
        );

        TextView txtGame = v.findViewById(R.id.txtGame);
        EditText edtGameId = v.findViewById(R.id.edtGameName);
        TextView btnConfirm = v.findViewById(R.id.btnConfirmJoin);

        txtGame.setText(game.toUpperCase());

        btnConfirm.setOnClickListener(view -> {

            String gameId = edtGameId.getText()
                    .toString()
                    .trim();

            if (!gameId.isEmpty()) {
                callback.onConfirm(gameId);
                dismiss();
            }
        });

        return v;
    }
}
