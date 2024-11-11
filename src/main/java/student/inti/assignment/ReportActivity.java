package student.inti.assignment;

import android.os.Bundle;
import android.view.View;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {
    private Spinner spinnerYear;
    private TextView tvTotalBalance, tvTotalExpensesIncome;
    private RecyclerView recyclerViewMonthlyReport;
    private MonthlyReportAdapter adapter;
    private List<MonthlyReport> reportList;
    private List<String> yearList;
    private FirebaseAuth mAuth;
    private DatabaseReference mExpenseDatabase, mIncomeDatabase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report");
        }

        initializeViews();
        setupFirebase();
        setupRecyclerView();
        setupYearSpinner();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close the activity when the back button is pressed
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void initializeViews() {
        spinnerYear = findViewById(R.id.spinnerYear);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalExpensesIncome = findViewById(R.id.tvTotalExpensesIncome);
        recyclerViewMonthlyReport = findViewById(R.id.recyclerViewMonthlyReport);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String dbUrl = "https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app";
            mExpenseDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("expenses");
            mIncomeDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("incomes");
        }
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        adapter = new MonthlyReportAdapter(reportList);
        recyclerViewMonthlyReport.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMonthlyReport.setAdapter(adapter);
    }

    private void setupYearSpinner() {
        yearList = generateYearList();
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearList);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedYear = Integer.parseInt(yearList.get(position));
                loadDataForYear(selectedYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set default selection to current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        spinnerYear.setSelection(yearAdapter.getPosition(String.valueOf(currentYear)));
    }

    private List<String> generateYearList() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear; i++) {
            years.add(String.valueOf(i));
        }
        return years;
    }

    private void loadDataForYear(int year) {
        reportList.clear();
        loadExpensesForYear(year);
    }

    private void loadExpensesForYear(int year) {
        mExpenseDatabase.child(String.valueOf(year)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Double> monthlyExpenses = new HashMap<>();
                for (DataSnapshot monthSnapshot : dataSnapshot.getChildren()) {
                    String month = monthSnapshot.getKey();
                    double totalExpense = calculateTotalForMonth(monthSnapshot);
                    monthlyExpenses.put(month, totalExpense);
                }
                loadIncomesForYear(year, monthlyExpenses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
            }
        });
    }

    private void loadIncomesForYear(int year, final Map<String, Double> monthlyExpenses) {
        mIncomeDatabase.child(String.valueOf(year)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Double> monthlyIncomes = new HashMap<>();
                for (DataSnapshot monthSnapshot : dataSnapshot.getChildren()) {
                    String month = monthSnapshot.getKey();
                    double totalIncome = calculateTotalForMonth(monthSnapshot);
                    monthlyIncomes.put(month, totalIncome);
                }
                updateReportList(monthlyExpenses, monthlyIncomes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
            }
        });
    }

    private double calculateTotalForMonth(DataSnapshot monthSnapshot) {
        double total = 0;
        for (DataSnapshot recordSnapshot : monthSnapshot.getChildren()) {
            Double amount = recordSnapshot.child("amount").getValue(Double.class);
            if (amount != null) {
                total += Math.abs(amount);
            }
        }
        return total;
    }

    private void updateReportList(Map<String, Double> monthlyExpenses, Map<String, Double> monthlyIncomes) {
        for (int i = 1; i <= 12; i++) {
            String monthKey = String.format("%02d", i);
            String monthName = getMonthName(monthKey);
            double expense = monthlyExpenses.getOrDefault(monthKey, 0.0);
            double income = monthlyIncomes.getOrDefault(monthKey, 0.0);
            double balance = income - expense;
            reportList.add(new MonthlyReport(monthName, expense, income, balance));
        }
        Collections.reverse(reportList);
        adapter.notifyDataSetChanged();
        updateTotalSummary();
    }

    private void updateTotalSummary() {
        double totalExpense = 0;
        double totalIncome = 0;
        for (MonthlyReport report : reportList) {
            totalExpense += report.getTotalExpense();
            totalIncome += report.getTotalIncome();
        }
        double totalBalance = totalIncome - totalExpense;

        tvTotalBalance.setText(String.format("Balance: RM%.2f", totalBalance));
        tvTotalExpensesIncome.setText(String.format("Expenses: RM%.2f | Income: RM%.2f", totalExpense, totalIncome));
    }

    private String getMonthName(String monthNumber) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        int index = Integer.parseInt(monthNumber) - 1;
        return months[index];
    }
}