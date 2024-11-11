package student.inti.assignment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

public class HomeActivity extends AppCompatActivity {

    private Spinner spinnerMonth, spinnerYear;
    private TextView tvTotalExpenses, tvTotalIncome, tvBalance;
    private FirebaseAuth mAuth;
    private DatabaseReference mExpenseDatabase, mIncomeDatabase , databaseRef;
    private ArrayList<Expense> expenses;
    private ArrayList<Income> incomes;
    private ExpenseAdapter expenseAdapter;
    private IncomeAdapter incomeAdapter;
    private ImageButton userButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        userButton = findViewById(R.id.userButton);

        prefs = getSharedPreferences("HomeActivityPrefs", MODE_PRIVATE);

        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvBalance = findViewById(R.id.tvBalance);

        // Find the Add button and set its click listener
        Button btnAddNew = findViewById(R.id.btnAddNew);
        btnAddNew.setOnClickListener(v -> {
            storeSelectedMonthYear();  // Save the selected month and year
            startActivity(new Intent(HomeActivity.this, AddExpenseActivity.class));
        });

// Find the User button and set its click listener
        ImageButton userButton = findViewById(R.id.userButton);
        userButton.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

// Find the Report button (ImageView) and set its click listener
        Button btnReport = findViewById(R.id.btnReport);
        btnReport.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ReportActivity.class)));

// Find the Chart button (ImageView) and set its click listener
        Button btnChart = findViewById(R.id.btnChart);
        btnChart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChartActivity.class)));

        userButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        if (currentUser != null) {
            String userId = currentUser.getUid();
            mExpenseDatabase = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(userId).child("expenses");
            mIncomeDatabase = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users").child(userId).child("incomes");
            databaseRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(userId);

            ListView listViewExpenses = findViewById(R.id.listViewExpenses);
            ListView listViewIncomes = findViewById(R.id.listViewIncomes);

            expenses = new ArrayList<>();
            incomes = new ArrayList<>();
            expenseAdapter = new ExpenseAdapter(this, expenses);
            incomeAdapter = new IncomeAdapter(this, incomes);

            listViewExpenses.setAdapter(expenseAdapter);
            listViewIncomes.setAdapter(incomeAdapter);

            spinnerMonth = findViewById(R.id.spinnerMonth);
            spinnerYear = findViewById(R.id.spinnerYear);

            setupMonthYearSpinners();

            loadExpenses();
            loadIncomes();
            loadProfilePicture();

            setupListViewListeners(listViewExpenses, listViewIncomes);

            // Set up listeners to edit expenses and incomes
            listViewExpenses.setOnItemClickListener((parent, view, position, id) -> {
                Expense selectedExpense = expenses.get(position);
                Intent editExpenseIntent = new Intent(HomeActivity.this, EditExpenseActivity.class);
                editExpenseIntent.putExtra("description", selectedExpense.getDescription());
                editExpenseIntent.putExtra("amount", selectedExpense.getAmount());
                editExpenseIntent.putExtra("date", selectedExpense.getDate());
                editExpenseIntent.putExtra("category", selectedExpense.getCategory());
                editExpenseIntent.putExtra("expenseId", selectedExpense.getId());
                editExpenseIntent.putExtra("IncomeId", selectedExpense.getId());
                startActivity(editExpenseIntent);
            });

            listViewIncomes.setOnItemClickListener((parent, view, position, id) -> {
                Income selectedIncome = incomes.get(position);
                Intent editIncomeIntent = new Intent(HomeActivity.this, EditExpenseActivity.class);
                editIncomeIntent.putExtra("description", selectedIncome.getDescription());
                editIncomeIntent.putExtra("amount", selectedIncome.getAmount());
                editIncomeIntent.putExtra("date", selectedIncome.getDate());
                editIncomeIntent.putExtra("category", selectedIncome.getCategory());
                editIncomeIntent.putExtra("incomeId", selectedIncome.getId());
                startActivity(editIncomeIntent);
            });
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        restoreSelectedMonthYear();
        loadExpenses();
        loadIncomes();
    }

    private void restoreSelectedMonthYear() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);
        // Retrieve saved month position and year, or use current month/year if not saved
        int savedMonthPosition = prefs.getInt("selectedMonthPosition", Calendar.getInstance().get(Calendar.MONTH));
        String savedYear = prefs.getString("selectedYear", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        // Set the spinners to the saved values
        spinnerMonth.setSelection(savedMonthPosition);
        int yearPosition = ((ArrayAdapter<String>) spinnerYear.getAdapter()).getPosition(savedYear);
        if (yearPosition >= 0) {
            spinnerYear.setSelection(yearPosition);
        }
    }

    private void storeSelectedMonthYear() {
        int selectedMonthPosition = spinnerMonth.getSelectedItemPosition();
        String selectedYear = spinnerYear.getSelectedItem().toString();

        prefs.edit()
                .putInt("selectedMonthPosition", selectedMonthPosition)
                .putString("selectedYear", selectedYear)
                .apply();
    }

    private void setupMonthYearSpinners() {
        // Set up month and year spinners
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getMonths());
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getYears());
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                storeSelectedMonthYear();
                loadExpenses();
                loadIncomes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spinnerMonth.setOnItemSelectedListener(listener);
        spinnerYear.setOnItemSelectedListener(listener);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences("HomeActivityPrefs", MODE_PRIVATE).edit().clear().apply();
                loadExpenses();
                loadIncomes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences("HomeActivityPrefs", MODE_PRIVATE).edit().clear().apply();
                loadExpenses();
                loadIncomes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

    }

    private ArrayList<String> getMonths() {
        ArrayList<String> months = new ArrayList<>();
        months.add("January"); months.add("February"); months.add("March"); months.add("April");
        months.add("May"); months.add("June"); months.add("July"); months.add("August");
        months.add("September"); months.add("October"); months.add("November"); months.add("December");
        return months;
    }

    private ArrayList<String> getYears() {
        ArrayList<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(String.valueOf(i));
        }
        return years;
    }

    private void setupListViewListeners(ListView listViewExpenses, ListView listViewIncomes) {
        listViewExpenses.setOnItemClickListener((parent, view, position, id) -> {
            Expense selectedExpense = expenses.get(position);
            Intent editExpenseIntent = new Intent(HomeActivity.this, EditExpenseActivity.class);
            editExpenseIntent.putExtra("description", selectedExpense.getDescription());
            editExpenseIntent.putExtra("amount", selectedExpense.getAmount());
            editExpenseIntent.putExtra("date", selectedExpense.getDate());
            editExpenseIntent.putExtra("category", selectedExpense.getCategory());
            editExpenseIntent.putExtra("expenseId", selectedExpense.getId());
            startActivity(editExpenseIntent);
        });

        listViewIncomes.setOnItemClickListener((parent, view, position, id) -> {
            Income selectedIncome = incomes.get(position);
            Intent editIncomeIntent = new Intent(HomeActivity.this, EditExpenseActivity.class);
            editIncomeIntent.putExtra("description", selectedIncome.getDescription());
            editIncomeIntent.putExtra("amount", selectedIncome.getAmount());
            editIncomeIntent.putExtra("date", selectedIncome.getDate());
            editIncomeIntent.putExtra("category", selectedIncome.getCategory());
            editIncomeIntent.putExtra("incomeId", selectedIncome.getId());
            startActivity(editIncomeIntent);
        });
    }

    private void loadExpenses() {
        String selectedYear = spinnerYear.getSelectedItem().toString();
        String selectedMonth = String.format("%02d", spinnerMonth.getSelectedItemPosition() + 1);

        mExpenseDatabase.child(selectedYear).child(selectedMonth).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                expenses.clear();
                double totalExpenses = 0.0;
                for (DataSnapshot expenseSnapshot : dataSnapshot.getChildren()) {
                    Expense expense = expenseSnapshot.getValue(Expense.class);
                    if (expense != null) {
                        expense.setId(expenseSnapshot.getKey());
                        expenses.add(expense);
                        totalExpenses += expense.getAmount();
                    }
                }
                tvTotalExpenses.setText(String.format("Expenses: RM%.2f", totalExpenses));
                calculateBalance();
                expenseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loadIncomes() {
        String selectedYear = spinnerYear.getSelectedItem().toString();
        String selectedMonth = String.format("%02d", spinnerMonth.getSelectedItemPosition() + 1);

        mIncomeDatabase.child(selectedYear).child(selectedMonth).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                incomes.clear();
                double totalIncome = 0.0;
                for (DataSnapshot incomeSnapshot : dataSnapshot.getChildren()) {
                    Income income = incomeSnapshot.getValue(Income.class);
                    if (income != null) {
                        income.setId(incomeSnapshot.getKey());
                        incomes.add(income);
                        totalIncome += income.getAmount();
                    }
                }
                tvTotalIncome.setText(String.format("Income: RM%.2f", totalIncome));
                calculateBalance();
                incomeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void calculateBalance() {
        String totalExpensesStr = tvTotalExpenses.getText().toString().replace("Expenses: RM", "");
        String totalIncomeStr = tvTotalIncome.getText().toString().replace("Income: RM", "");
        double totalExpenses = Double.parseDouble(totalExpensesStr);
        double totalIncome = Double.parseDouble(totalIncomeStr);
        double balance = totalIncome + totalExpenses;

        tvBalance.setText(String.format("Balance: RM%.2f", balance));
    }

    private void loadProfilePicture() {
        databaseRef.child("profilePicture").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String encodedImage = snapshot.getValue(String.class);
                if (encodedImage != null) {
                    // Decode Base64 image and load into userButton
                    byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                    Glide.with(HomeActivity.this)
                            .asBitmap()
                            .load(decodedBytes)  // Load byte array directly
                            .placeholder(R.drawable.default_profile)  // Placeholder
                            .error(R.drawable.default_profile)  // Error image
                            .circleCrop()  // Circular crop for rounded image
                            .into(userButton);
                } else {
                    userButton.setImageResource(R.drawable.default_profile); // Default if no image
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load profile picture", Toast.LENGTH_SHORT).show();
            }
        });
    }

}