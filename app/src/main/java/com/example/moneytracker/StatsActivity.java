package com.example.moneytracker;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {
    LinearLayout layoutYearly, layoutMonthly;
    PieChart pieChart;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        layoutYearly = findViewById(R.id.layoutYearly);
        layoutMonthly = findViewById(R.id.layoutMonthly);
        pieChart = findViewById(R.id.pieChart);

        dbHelper = new DatabaseHelper(this);

        // La deschiderea paginii, încărcăm implicit graficul pentru luna curentă
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        updatePieChart(currentMonth);

        populateSection(layoutYearly, dbHelper.getYearlyStats(), true);
        populateSection(layoutMonthly, dbHelper.getMonthlyStats(), false);
    }

    /**
     * Actualizează PieChart-ul pentru perioada selectată (lună sau an).
     */
    private void updatePieChart(String period) {
        ArrayList<String> data = dbHelper.getCategoryStats(period);
        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (String item : data) {
            String[] parts = item.split(";");
            String category = parts[0];

            // REZOLVARE 1: Folosim Math.abs pentru a transforma negativ în pozitiv
            // ca librăria să poată desena și colora felia corect.
            float amount = Math.abs(Float.parseFloat(parts[1]));

            if (category.isEmpty()) category = getString(R.string.cat_other);
            pieEntries.add(new PieEntry(amount, category));
            colors.add(CategoryUtils.getCategoryColor(category));
        }

        if (pieEntries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            return;
        } else {
            pieChart.setVisibility(View.VISIBLE);
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, getString(R.string.lbl_empty));
        dataSet.setColors(colors);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.white));
        dataSet.setValueTextSize(14f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);

        // REZOLVARE 2: Punem textul din mijloc să reflecte exact perioada pe care am dat click
        pieChart.setCenterText(period);

        pieChart.setCenterTextColor(ContextCompat.getColor(this, R.color.white)); // Făcut alb pentru Dark Mode
        pieChart.setHoleColor(ContextCompat.getColor(this, R.color.blackout)); // Făcut în ton cu fundalul
        pieChart.setEntryLabelColor(ContextCompat.getColor(this, R.color.white));
        pieChart.setEntryLabelTextSize(12f);
        pieChart.getLegend().setEnabled(false);
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    private void populateSection(LinearLayout container, ArrayList<String> dataList, boolean isYearly) {
        if (dataList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.msg_error_no_transaction);
            empty.setTextColor(ContextCompat.getColor(this, R.color.gray));
            container.addView(empty);
            return;
        }

        for (String record : dataList) {
            String[] parts = record.split(";");
            final String periodLabel = parts[0];
            double income = Double.parseDouble(parts[1]);
            double expense = Double.parseDouble(parts[2]);
            double balance = income + expense;

            LinearLayout card = new LinearLayout(this);
            int color = isYearly ? R.color.mettalic_black : R.color.blackout;
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(40, 40, 40, 40);
            card.setBackgroundColor(ContextCompat.getColor(this, color));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 20);
            card.setLayoutParams(params);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(periodLabel);
            tvTitle.setTextSize(isYearly ? 22 : 18);
            tvTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvTitle.setTypeface(null, Typeface.BOLD);

            TextView tvDetails = new TextView(this);
            tvDetails.setText(String.format(getString(R.string.lbl_income_expense), income, expense));
            tvDetails.setTextColor(ContextCompat.getColor(this, R.color.lightgray));
            tvDetails.setTextSize(14);
            tvDetails.setPadding(0, 10, 0, 10);

            TextView tvBalance = new TextView(this);
            int colorBalance = balance >= 0 ? R.color.green : R.color.red;
            tvBalance.setText(String.format(getString(R.string.lbl_total), String.format(getString(R.string.lbl_balance), balance)));
            tvBalance.setTextSize(16);
            tvBalance.setTypeface(null, Typeface.BOLD);
            tvBalance.setTextColor(ContextCompat.getColor(this, colorBalance));
            tvBalance.setGravity(Gravity.END);

            card.addView(tvTitle);
            card.addView(tvDetails);
            card.addView(tvBalance);

            if (!isYearly) {
                // Dacă este card lunar, la click actualizăm graficul ȘI deschidem detaliile
                LinearLayout hiddenDetails = new LinearLayout(this);
                hiddenDetails.setOrientation(LinearLayout.VERTICAL);
                hiddenDetails.setVisibility(View.GONE);
                hiddenDetails.setPadding(10, 20, 10, 0);

                card.addView(hiddenDetails);

                card.setOnClickListener(v -> {
                    // Actualizare grafic
                    updatePieChart(periodLabel);

                    // Afișare/Ascundere tranzacții
                    if (hiddenDetails.getVisibility() == View.VISIBLE) {
                        hiddenDetails.setVisibility(View.GONE);
                    } else {
                        loadDetailsIntoView(hiddenDetails, periodLabel);
                        hiddenDetails.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                // Dacă este card anual, la click DOAR actualizăm graficul (arată cheltuielile pe tot anul)
                card.setOnClickListener(v -> {
                    updatePieChart(periodLabel);
                });
            }
            container.addView(card);
        }
    }

    private void loadDetailsIntoView(LinearLayout container, String period) {
        container.removeAllViews();

        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
        line.setBackgroundColor(ContextCompat.getColor(this, R.color.darkgray));
        container.addView(line);

        ArrayList<Transaction> list = dbHelper.getTransactionsForPeriod(period);

        if (list.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText(R.string.msg_error_no_transaction);
            tv.setTextColor(ContextCompat.getColor(this, R.color.gray));
            tv.setPadding(0, 20, 0, 0);
            container.addView(tv);
            return;
        }

        for (Transaction t : list) {
            String day = t.date.length() >= 10 ? t.date.substring(8, 10) : getString(R.string.lbl_empty);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 15, 0, 15);

            TextView tvDay = new TextView(this);
            tvDay.setText(day);
            tvDay.setTextColor(ContextCompat.getColor(this, R.color.lightgray));
            tvDay.setTextSize(14);
            tvDay.setTypeface(null, Typeface.BOLD);
            tvDay.setPadding(0, 0, 15, 0);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(t.description);
            tvDesc.setTextColor(ContextCompat.getColor(this, R.color.white));
            tvDesc.setTextSize(15);
            tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tvAmount = new TextView(this);
            tvAmount.setText(String.format(getString(R.string.lbl_amount), t.amount));
            tvAmount.setTextSize(15);
            tvAmount.setTypeface(null, Typeface.BOLD);

            if(t.amount < 0) {
                tvAmount.setTextColor(ContextCompat.getColor(this, R.color.red));
            } else {
                tvAmount.setTextColor(ContextCompat.getColor(this, R.color.green));
                tvAmount.setText(getString(R.string.lbl_plus_sign) + String.format(getString(R.string.lbl_amount), t.amount));
            }

            row.addView(tvDay);
            row.addView(tvDesc);
            row.addView(tvAmount);
            container.addView(row);
        }
    }
}