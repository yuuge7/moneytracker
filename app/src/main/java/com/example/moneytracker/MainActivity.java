package com.example.moneytracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private ArrayList<Transaction> transactionList;
    private DatabaseHelper dbHelper;

    private TextView tvTotalBalance, tvIncome, tvExpense, tvDashboardLabel;
    private EditText etDesc, etAmount, etSearch;
    private Spinner spinnerCategory;
    private Button btnAdd, btnDate;
    private RadioButton rbExpense, rbIncome;

    private String selectedDateString = "";
    private String selectedCategoryFilter = "Toate";

    // LISTA ACTUALIZATĂ DE CATEGORII (Exact cele cerute)
    private final String[] categories = {
            "Mâncare",
            "Cumpărături",
            "Transport",
            "Chirie",
            "Salariu",
            "Distracție",
            "Educație",
            "Altele"
    };

    private int currentEditId = -1; // -1 înseamnă "Adăugare Nouă"

    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        setupLaunchers();
        setupDatabaseAndRecyclerView();
        setupCategoryFilters();
        setupListeners();

        resetDateToToday();
        loadData();
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvDashboardLabel = findViewById(R.id.tvDashboardLabel);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        recyclerView = findViewById(R.id.recyclerView);
        etDesc = findViewById(R.id.etDesc);
        etAmount = findViewById(R.id.etAmount);
        etSearch = findViewById(R.id.etSearch);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAdd = findViewById(R.id.btnAdd);
        btnDate = findViewById(R.id.btnDate);
        rbExpense = findViewById(R.id.rbExpense);
        rbIncome = findViewById(R.id.rbIncome);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.WHITE);
                return view;
            }
        };
        spinnerCategory.setAdapter(spinnerAdapter);
    }

    private void setupLaunchers() {
        backupLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                                String json = dbHelper.getAllDataAsJSON();
                                if (os != null) {
                                    os.write(json.getBytes(StandardCharsets.UTF_8));
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Backup Salvat!", Toast.LENGTH_LONG).show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Eroare la export!", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
        );

        restoreLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try (InputStream is = getContentResolver().openInputStream(uri);
                                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line);
                                }

                                dbHelper.importDataFromJSON(sb.toString());

                                runOnUiThread(() -> {
                                    loadData();
                                    Toast.makeText(MainActivity.this, "Date importate cu succes!", Toast.LENGTH_LONG).show();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Eroare la import!", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
        );
    }

    private void setupDatabaseAndRecyclerView() {
        dbHelper = new DatabaseHelper(this);
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(this, transactionList, dbHelper, this::loadData);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupCategoryFilters() {
        LinearLayout layoutFilters = findViewById(R.id.layoutCategoryFilters);

        findViewById(R.id.btnFilterAll).setOnClickListener(v -> {
            selectedCategoryFilter = "Toate";
            filterTransactions(etSearch.getText().toString());
        });

        for (String cat : categories) {
            Button btn = new Button(this, null, android.R.attr.borderlessButtonStyle);
            btn.setText(cat);
            btn.setAllCaps(false);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 15, 0);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                selectedCategoryFilter = cat;
                filterTransactions(etSearch.getText().toString());
            });
            layoutFilters.addView(btn);
        }
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnDate.setOnClickListener(v -> showDatePicker());
        btnAdd.setOnClickListener(v -> addTransaction());
    }

    public void startEditMode(Transaction t) {
        currentEditId = t.id;

        etDesc.setText(t.description);
        etAmount.setText(String.valueOf(Math.abs(t.amount)));

        if (t.amount < 0) {
            rbExpense.setChecked(true);
        } else {
            rbIncome.setChecked(true);
        }

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(t.category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        selectedDateString = t.date;
        btnDate.setText("📅 " + selectedDateString);

        btnAdd.setText("Salvează");
        btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#03DAC5")));
    }

    private void filterTransactions(String text) {
        ArrayList<Transaction> filteredList = new ArrayList<>();
        for (Transaction item : transactionList) {
            boolean matchesText = item.description.toLowerCase().contains(text.toLowerCase());
            boolean matchesCategory = selectedCategoryFilter.equals("Toate") ||
                    item.category.equals(selectedCategoryFilter);
            if (matchesText && matchesCategory) filteredList.add(item);
        }
        adapter.filterList(filteredList);
        updateDashboard();
    }

    private void updateDashboard() {
        String searchQuery = etSearch.getText().toString().trim();
        boolean isSearching = !searchQuery.isEmpty() || !selectedCategoryFilter.equals("Toate");

        String currentMonthKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        String displayMonth = new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date());
        displayMonth = displayMonth.substring(0, 1).toUpperCase() + displayMonth.substring(1);

        double total = 0, income = 0, expense = 0;
        ArrayList<Transaction> currentDisplayedList = adapter.getCurrentList();

        for (Transaction t : currentDisplayedList) {
            if (isSearching) {
                total += t.amount;
                if (t.amount > 0) income += t.amount;
                else expense += t.amount;
            } else {
                if (t.date.startsWith(currentMonthKey)) {
                    total += t.amount;
                    if (t.amount > 0) income += t.amount;
                    else expense += t.amount;
                }
            }
        }

        if (isSearching) {
            String filterInfo = !searchQuery.isEmpty() ? searchQuery : selectedCategoryFilter;
            tvDashboardLabel.setText(getString(R.string.lbl_search_stats, filterInfo));
            tvDashboardLabel.setTextColor(Color.parseColor("#03DAC5"));
        } else {
            tvDashboardLabel.setText(getString(R.string.lbl_monthly_balance, displayMonth));
            tvDashboardLabel.setTextColor(Color.parseColor("#B0B0B0"));
        }

        tvTotalBalance.setText(String.format(Locale.US, "%.2f RON", total));
        tvIncome.setText(getString(R.string.lbl_income, income));
        tvExpense.setText(getString(R.string.lbl_expense, expense));
    }

    private void loadData() {
        transactionList.clear();
        transactionList.addAll(dbHelper.getAllTransactions());
        filterTransactions(etSearch.getText().toString());
    }

    private void addTransaction() {
        String desc = etDesc.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";

        if (desc.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "Completează toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amountInput = Math.abs(Double.parseDouble(amountStr));
            if (rbExpense.isChecked()) amountInput = -amountInput;

            if (currentEditId == -1) {
                dbHelper.addTransaction(desc, amountInput, category, selectedDateString);
            } else {
                dbHelper.updateTransaction(currentEditId, desc, amountInput, category, selectedDateString);
                Toast.makeText(this, "Modificare salvată!", Toast.LENGTH_SHORT).show();

                currentEditId = -1;
                btnAdd.setText("Adaugă");
                btnAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#BB86FC")));
            }

            etDesc.setText("");
            etAmount.setText("");
            resetDateToToday();
            loadData();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Suma introdusă nu este validă!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetDateToToday() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDateString = sdf.format(new Date());
        btnDate.setText("📅 " + selectedDateString);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateString = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                    btnDate.setText("📅 " + selectedDateString);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // AM ELIMINAT complet restricțiile de selectare a datelor (setMaxDate / setMinDate)
        datePickerDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_stats) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        } else if (id == R.id.action_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "money_backup.json");
            backupLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_import) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            restoreLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}