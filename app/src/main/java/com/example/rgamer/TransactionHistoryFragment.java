package com.example.rgamer;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryFragment extends Fragment {

    RecyclerView recyclerView;
    FirebaseFirestore db;
    FirebaseAuth auth;
    List<DocumentSnapshot> list = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_transaction_history_fragment, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadHistory();

        return view;
    }

    private void loadHistory() {
        db.collection("redeem_requests")
                .whereEqualTo("uid", auth.getCurrentUser().getUid())
                .orderBy("created_at", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(qs -> {
                    list = qs.getDocuments();
                    recyclerView.setAdapter(new HistoryAdapter());
                });
    }

    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int i) {
            DocumentSnapshot d = list.get(i);
            h.txtType.setText(d.getString("type"));
            h.txtAmount.setText(d.getString("amount"));
            h.txtStatus.setText(d.getString("status"));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView txtType, txtAmount, txtStatus;
            Holder(View v) {
                super(v);
                txtType = v.findViewById(R.id.txtType);
                txtAmount = v.findViewById(R.id.txtAmount);
                txtStatus = v.findViewById(R.id.txtStatus);
            }
        }
    }
}
