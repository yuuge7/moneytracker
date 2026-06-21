package com.example.moneytracker;

public class Transaction {
    int id;
    String description;
    double amount;
    String category;
    String date;

    public Transaction(int id, String description, double amount, String category, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }
}