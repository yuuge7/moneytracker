# MoneyTracker 💰

MoneyTracker is a lightweight and intuitive Android application designed to help users manage their personal finances. Track your income and expenses, visualize your spending patterns, and keep your data safe with built-in backup features.

## 🚀 Features

-   **Transaction Tracking**: Easily add, edit, or delete transactions. Supports negative values for expenses and positive for income.
-   **Dashboard**: Real-time summary of your Monthly Balance, Total Income, and Total Expenses.
-   **Advanced Statistics**:
    -   Interactive **PieChart** (powered by MPAndroidChart) showing expense distribution.
    -   Consistent category coloring (e.g., Transport is always Blue, Food is always Orange).
-   **History & Reports**:
    -   **Yearly View**: Summary of financial performance per year.
    -   **Monthly View**: Drill down into specific months to see daily transaction details.
-   **Search & Filtering**: Quick search by description and quick-filter buttons for categories (Mâncare, Cumpărături, Transport, etc.).
-   **Data Management**:
    -   **Export**: Save all your data to a JSON file.
    -   **Import**: Restore your transactions from a backup file.
-   **Dark Mode**: Fully supports Android's system-wide Dark Theme.

## 🛠 Tech Stack

-   **Language**: Java
-   **Architecture**: MVC (Model-View-Controller)
-   **Database**: SQLite (local storage via `SQLiteOpenHelper`)
-   **UI**: Material Design 3, RecyclerView, ConstraintLayout
-   **Libraries**:
    -   `MPAndroidChart`: For high-quality financial visualizations.
    -   `Material Components`: For modern UI elements.

## 📦 Project Structure

-   `MainActivity.java`: The core hub for transaction entry and the dashboard.
-   `StatsActivity.java`: Handles charts and historical reports.
-   `DatabaseHelper.java`: Manages SQLite operations and JSON backup/restore logic.
-   `TransactionAdapter.java`: Custom adapter for the transaction list.
-   `CategoryUtils.java`: Centralized utility for category icons and color consistency.
-   `Transaction.java`: Data model for financial records.

## ⚙️ Requirements & Installation

1.  **Android Studio**: Version Hedgehog or newer recommended.
2.  **SDK**: Minimum SDK 24 (Android 7.0), Target SDK 36.
3.  **Setup**:
    -   Clone this repository.
    -   Open the project in Android Studio.
    -   Wait for Gradle Sync to complete.
    -   Build and run on your device.



