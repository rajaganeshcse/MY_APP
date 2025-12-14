package com.example.rgamer;

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
import androidx.fragment.app.Fragment;

import com.example.rgamer.R;

public class GameFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_game, container, false);

        view.findViewById(R.id.lytHowToWin).setOnClickListener(v ->
                Toast.makeText(getContext(), "How to win coins", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardFreeFire).setOnClickListener(v ->
                Toast.makeText(getContext(), "FreeFire Tournament", Toast.LENGTH_SHORT).show()
        );

        view.findViewById(R.id.cardPubg).setOnClickListener(v ->
                Toast.makeText(getContext(), "PUBG Tournament", Toast.LENGTH_SHORT).show()
        );

        View daily = view.findViewById(R.id.taskDailyBonus);
        ((TextView) daily.findViewById(R.id.txtTitle)).setText("Daily Bonus");
        ((Button) daily.findViewById(R.id.btnAction)).setText("Get");

        View video = view.findViewById(R.id.taskVideo);
        ((TextView) video.findViewById(R.id.txtTitle)).setText("Video Task");
        ((Button) video.findViewById(R.id.btnAction)).setText("Watch");
        ((ImageView) video.findViewById(R.id.imgIcon))
                .setImageResource(R.drawable.ic_play);

        return view;
    }
}
