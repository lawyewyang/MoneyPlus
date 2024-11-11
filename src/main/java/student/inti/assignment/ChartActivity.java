package student.inti.assignment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ChartActivity extends AppCompatActivity {

    private PieChart expensePieChart, incomePieChart;
    private DatabaseReference mExpenseDatabase, mIncomeDatabase;
    private FirebaseAuth mAuth;
    private LinearLayout expenseLegendLayout, incomeLegendLayout;
    private LinearLayout expenseDetailLayout, incomeDetailLayout;
    private Spinner yearSpinner, monthSpinner;

    private String selectedYear, selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chart"); // Optional: Set title for the screen
        }

        // Initialize views
        expensePieChart = findViewById(R.id.expensePieChart);
        incomePieChart = findViewById(R.id.incomePieChart);
        expenseLegendLayout = findViewById(R.id.expenseLegendLayout);
        incomeLegendLayout = findViewById(R.id.incomeLegendLayout);
        expenseDetailLayout = findViewById(R.id.expenseDetailLayout);
        incomeDetailLayout = findViewById(R.id.incomeDetailLayout);
        yearSpinner = findViewById(R.id.yearSpinner);
        monthSpinner = findViewById(R.id.monthSpinner);

        // Get Firebase references
        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        mExpenseDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("expenses");
        mIncomeDatabase = FirebaseDatabase.getInstance().getReference("users").child(userId).child("incomes");

        // Populate spinners
        setupYearSpinner();
        setupMonthSpinner();

        // Set default to the real-time current year and month
        Calendar calendar = Calendar.getInstance();
        selectedYear = String.valueOf(calendar.get(Calendar.YEAR));
        selectedMonth = String.format("%02d", calendar.get(Calendar.MONTH) + 1); // Get month as a 2-digit string

        // Set the spinner default selections to current year and month
        yearSpinner.setSelection(getYearPosition(selectedYear));
        monthSpinner.setSelection(Integer.parseInt(selectedMonth) - 1);

        // Load data for the current month and year
        loadDataForSelectedDate();

        // Set listeners to reload data whenever the year or month is changed by the user
        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = yearSpinner.getSelectedItem().toString();
                loadDataForSelectedDate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = String.format("%02d", position + 1); // Format month as 2-digit
                loadDataForSelectedDate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity and go back to the previous screen
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupYearSpinner() {
        ArrayList<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 2020; i <= currentYear; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
    }

    private void setupMonthSpinner() {
        ArrayList<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            months.add(String.format("%02d", i));  // Format as 2-digit month
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
    }

    private int getYearPosition(String year) {
        ArrayAdapter<String> yearAdapter = (ArrayAdapter<String>) yearSpinner.getAdapter();
        return yearAdapter.getPosition(year);
    }

    private void loadDataForSelectedDate() {
        loadExpenseData();
        loadIncomeData();
    }

    private void loadExpenseData() {
        mExpenseDatabase.child(selectedYear).child(selectedMonth).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Double> categoryTotals = new HashMap<>();
                double totalAmount = 0.0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Expense expense = snapshot.getValue(Expense.class);
                    if (expense != null) {
                        String category = expense.getCategory();
                        double amount = expense.getAmount();
                        totalAmount += amount;

                        categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                    }
                }

                expensePieChart.clearChart();
                expenseLegendLayout.removeAllViews();
                expenseDetailLayout.removeAllViews();

                for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                    String category = entry.getKey();
                    double amount = entry.getValue();
                    int color = getCategoryColor(category);
                    double percentage = (amount / totalAmount) * 100;

                    expensePieChart.addPieSlice(new PieModel(category, (float) amount, color));

                    addLegendEntry(expenseLegendLayout, category, color, percentage);
                    addDetailedEntry(expenseDetailLayout, category, color, percentage, amount);
                }

                expensePieChart.startAnimation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChartActivity.this, "Failed to load expenses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadIncomeData() {
        mIncomeDatabase.child(selectedYear).child(selectedMonth).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Double> categoryTotals = new HashMap<>();
                double totalAmount = 0.0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Income income = snapshot.getValue(Income.class);
                    if (income != null) {
                        String category = income.getCategory();
                        double amount = income.getAmount();
                        totalAmount += amount;

                        categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                    }
                }

                incomePieChart.clearChart();
                incomeLegendLayout.removeAllViews();
                incomeDetailLayout.removeAllViews();

                for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                    String category = entry.getKey();
                    double amount = entry.getValue();
                    int color = getCategoryColor(category);
                    double percentage = (amount / totalAmount) * 100;

                    incomePieChart.addPieSlice(new PieModel(category, (float) amount, color));

                    addLegendEntry(incomeLegendLayout, category, color, percentage);
                    addDetailedEntry(incomeDetailLayout, category, color, percentage, amount);
                }

                incomePieChart.startAnimation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChartActivity.this, "Failed to load incomes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addLegendEntry(LinearLayout legendLayout, String category, int color, double percentage) {
        TextView legendEntry = new TextView(this);
        legendEntry.setText(String.format("%s: %.2f%%", category, percentage));
        legendEntry.setTextColor(color);
        legendLayout.addView(legendEntry);
    }

    private void addDetailedEntry(LinearLayout detailLayout, String category, int color, double percentage, double amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);

        ImageView icon = new ImageView(this);
        int iconResource = getCategoryIcon(category);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            iconParams.weight = 0;
            icon.setLayoutParams(iconParams);
        } else {
            icon.setVisibility(View.GONE);
        }
        row.addView(icon);

        TextView categoryText = new TextView(this);
        categoryText.setText(category);
        categoryText.setTextColor(color);
        categoryText.setPadding(8, 0, 0, 0);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        categoryParams.weight = 2;
        categoryText.setLayoutParams(categoryParams);
        row.addView(categoryText);

        TextView percentageText = new TextView(this);
        percentageText.setText(String.format("%.2f%%", percentage));
        percentageText.setPadding(8, 0, 0, 0);
        LinearLayout.LayoutParams percentageParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        percentageParams.weight = 1;
        percentageText.setLayoutParams(percentageParams);
        row.addView(percentageText);

        TextView amountLabel = new TextView(this);
        amountLabel.setText("Amount:");
        amountLabel.setPadding(8, 0, 0, 0);
        LinearLayout.LayoutParams amountLabelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountLabelParams.weight = 0;
        amountLabel.setLayoutParams(amountLabelParams);
        row.addView(amountLabel);

        TextView amountText = new TextView(this);
        amountText.setText(String.format("%.2f", amount));
        amountText.setPadding(8, 0, 0, 0);
        LinearLayout.LayoutParams amountParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        amountParams.weight = 1;
        amountText.setLayoutParams(amountParams);
        row.addView(amountText);

        detailLayout.addView(row);
    }

    private int getCategoryColor(String category) {
        return Color.rgb((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256));
    }

    private int getCategoryIcon(String category) {
        Log.d("Category", "Category: " + category);
        switch (category.toLowerCase()) {
            case "food":
                return R.drawable.ic_food;
            case "transport":
                return R.drawable.ic_transport;
            case "education":
                return R.drawable.ic_education;
            case "parking":
                return R.drawable.ic_parking;
            case "travel":
                return R.drawable.ic_travel;
            case "shopping":
                return R.drawable.ic_shopping;
            case "dessert":
                return R.drawable.ic_dessert;
            case "entertainment":
                return R.drawable.ic_entertainment;
            case "salary":
                return R.drawable.ic_salary;
            case "pocket_money":
                return R.drawable.ic_pocketmoney;
            case "part_time":
                return R.drawable.ic_parttime;
            default:
                Log.e("Unknown Category", "No icon found for category: " + category);
                return R.drawable.ic_default;
        }
    }
}
