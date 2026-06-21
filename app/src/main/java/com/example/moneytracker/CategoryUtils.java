package com.example.moneytracker;

import android.graphics.Color;

public class CategoryUtils {
    public static String getCategoryIcon(String category) {
        if (category == null) return "📝";
        switch (category) {
            case "Mâncare": return "🍔";
            case "Cumpărături": return "🛒";
            case "Salariu": return "💰";
            case "Chirie": return "🏠";
            case "Transport": return "🚗";
            case "Distracție": return "🎉";
            case "Educație": return "📚";
            case "Altele": return "📦";
            default: return "📝";
        }
    }

    public static int getCategoryColor(String category) {
        if (category == null) return Color.parseColor("#607D8B");
        switch (category) {
            case "Mâncare": return Color.parseColor("#FF9800"); // Portocaliu
            case "Cumpărături": return Color.parseColor("#009688"); // Teal
            case "Salariu": return Color.parseColor("#4CAF50"); // Verde
            case "Chirie": return Color.parseColor("#F44336");  // Roșu
            case "Transport": return Color.parseColor("#2196F3"); // Albastru
            case "Distracție": return Color.parseColor("#9C27B0"); // Violet
            case "Educație": return Color.parseColor("#3F51B5"); // Indigo
            case "Altele": return Color.parseColor("#607D8B");   // Gri Albăstrui
            default: return Color.parseColor("#607D8B");   // Gri pentru necunoscute
        }
    }
}
