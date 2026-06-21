package com.example.moneytracker;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    Context context;
    ArrayList<Transaction> list;
    DatabaseHelper dbHelper;
    Runnable onUpdateListener;

    public TransactionAdapter(Context context, ArrayList<Transaction> list, DatabaseHelper dbHelper, Runnable onUpdateListener) {
        this.context = context;
        this.list = list;
        this.dbHelper = dbHelper;
        this.onUpdateListener = onUpdateListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = list.get(position);

        holder.tvDesc.setText(t.description);
        holder.tvDate.setText(t.date);

        // Setăm suma și culoarea textului (Verde/Roșu)
        if (t.amount < 0) {
            holder.tvAmount.setText(String.format("%.2f RON", t.amount));
            holder.tvAmount.setTextColor(Color.parseColor("#CF6679")); // Roșu
        } else {
            holder.tvAmount.setText("+" + String.format("%.2f RON", t.amount));
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Verde
        }

        // --- PARTEA VIZUALĂ (ICONIȚE ȘI CULORI) ---
        holder.tvCategory.setText(CategoryUtils.getCategoryIcon(t.category));
        holder.tvCategory.setBackgroundColor(CategoryUtils.getCategoryColor(t.category));

        // Ștergere la click lung
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Șterge")
                    .setMessage("Ești sigur că vrei să ștergi tranzacția: " + t.description + "?")
                    .setPositiveButton("Da", (dialog, which) -> {
                        dbHelper.deleteTransaction(t.id);
                        list.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, list.size());
                        onUpdateListener.run(); // Face refresh și la Dashboard
                    })
                    .setNegativeButton("Nu", null)
                    .show();
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).startEditMode(t);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvDate, tvAmount, tvCategory;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }

    // --- FUNCȚII PENTRU BARA DE CĂUTARE ȘI FILTRE ---

    public void filterList(ArrayList<Transaction> filteredList) {
        this.list = filteredList;
        notifyDataSetChanged();
    }

    public ArrayList<Transaction> getCurrentList() {
        return this.list;
    }

    // --- FUNCȚII AJUTĂTOARE PENTRU DESIGN (S-au mutat în CategoryUtils) ---
}