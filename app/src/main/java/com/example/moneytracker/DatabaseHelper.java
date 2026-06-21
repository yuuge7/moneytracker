package com.example.moneytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MoneyTrackerDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "transactions";
    private static final String COL_ID = "id";
    private static final String COL_DESC = "description";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_DESC + " TEXT, " +
                COL_AMOUNT + " REAL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
        }
    }

    // --- OPERAȚIUNI DE BAZĂ (CRUD) ---

    public void addTransaction(String desc, double amount, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DESC, desc);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void updateTransaction(int id, String desc, double amount, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DESC, desc);
        values.put(COL_AMOUNT, amount);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);

        db.update(TABLE_NAME, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public ArrayList<Transaction> getAllTransactions() {
        ArrayList<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String desc = cursor.getString(1);
                double amount = cursor.getDouble(2);
                String category = cursor.getString(3);
                String date = cursor.getString(4);

                list.add(new Transaction(id, desc, amount, category, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- BACKUP ȘI RESTORE (JSON) ---

    public String getAllDataAsJSON() throws Exception {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        JSONArray jsonArray = new JSONArray();

        while (cursor.moveToNext()) {
            JSONObject obj = new JSONObject();
            obj.put(COL_DESC, cursor.getString(1));
            obj.put(COL_AMOUNT, cursor.getDouble(2));
            obj.put(COL_CATEGORY, cursor.getString(3));
            obj.put(COL_DATE, cursor.getString(4));
            jsonArray.put(obj);
        }
        cursor.close();
        db.close();
        return jsonArray.toString();
    }

    // METODĂ CORECTATĂ: Mapare exactă pentru desc, amnt, cat, date
    public void importDataFromJSON(String jsonString) throws Exception {
        JSONArray jsonArray = new JSONArray(jsonString);
        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_NAME);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                // 1. Mapare descriere
                String desc = "";
                if (obj.has("desc")) desc = obj.getString("desc");
                else if (obj.has("description")) desc = obj.getString("description");

                // 2. Mapare sumă (amnt)
                double amount = 0;
                if (obj.has("amnt")) amount = obj.getDouble("amnt");
                else if (obj.has("amount")) amount = obj.getDouble("amount");
                else if (obj.has("suma")) amount = obj.getDouble("suma");

                // 3. Mapare categorie
                String category = "";
                if (obj.has("cat")) category = obj.getString("cat");
                else if (obj.has("category")) category = obj.getString("category");
                else if (obj.has("categorie")) category = obj.getString("categorie");

                // 4. Mapare dată
                String date = "";
                if (obj.has("date")) date = obj.getString("date");
                else if (obj.has("data")) date = obj.getString("data");

                values.put(COL_DESC, desc);
                values.put(COL_AMOUNT, amount);
                values.put(COL_CATEGORY, category);
                values.put(COL_DATE, date);

                db.insert(TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // --- METODE PENTRU STATISTICI ȘI GRAFICE (StatsActivity) ---

    public ArrayList<String> getYearlyStats() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT substr(" + COL_DATE + ", 1, 4) as period, " +
                "SUM(CASE WHEN " + COL_AMOUNT + " > 0 THEN " + COL_AMOUNT + " ELSE 0 END) as income, " +
                "SUM(CASE WHEN " + COL_AMOUNT + " < 0 THEN " + COL_AMOUNT + " ELSE 0 END) as expense " +
                "FROM " + TABLE_NAME + " GROUP BY period ORDER BY period DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String period = cursor.getString(0);
                double income = cursor.getDouble(1);
                double expense = cursor.getDouble(2);
                list.add(period + ";" + income + ";" + expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public ArrayList<String> getMonthlyStats() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT substr(" + COL_DATE + ", 1, 7) as period, " +
                "SUM(CASE WHEN " + COL_AMOUNT + " > 0 THEN " + COL_AMOUNT + " ELSE 0 END) as income, " +
                "SUM(CASE WHEN " + COL_AMOUNT + " < 0 THEN " + COL_AMOUNT + " ELSE 0 END) as expense " +
                "FROM " + TABLE_NAME + " GROUP BY period ORDER BY period DESC";

        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                String period = cursor.getString(0);
                double income = cursor.getDouble(1);
                double expense = cursor.getDouble(2);
                list.add(period + ";" + income + ";" + expense);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public ArrayList<String> getCategoryStats(String period) {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") as total " +
                "FROM " + TABLE_NAME + " " +
                "WHERE " + COL_DATE + " LIKE ? AND " + COL_AMOUNT + " < 0 " +
                "GROUP BY " + COL_CATEGORY;

        Cursor cursor = db.rawQuery(query, new String[]{period + "%"});
        if (cursor.moveToFirst()) {
            do {
                String category = cursor.getString(0);
                double total = cursor.getDouble(1);
                list.add(category + ";" + total);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public ArrayList<Transaction> getTransactionsForPeriod(String period) {
        ArrayList<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COL_DATE + " LIKE ? ORDER BY " + COL_DATE + " DESC, " + COL_ID + " DESC", new String[]{period + "%"});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String desc = cursor.getString(1);
                double amount = cursor.getDouble(2);
                String category = cursor.getString(3);
                String date = cursor.getString(4);
                list.add(new Transaction(id, desc, amount, category, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}