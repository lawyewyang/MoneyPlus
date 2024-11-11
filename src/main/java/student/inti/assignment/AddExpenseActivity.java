package student.inti.assignment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;

public class AddExpenseActivity extends AppCompatActivity {

    private TextInputEditText etDescription, etAmount, etDate;
    private ExtendedFloatingActionButton btnSave;
    private GridLayout categoryGrid;
    private String selectedCategory = "";
    private SwitchMaterial switchType;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Calendar calendar;

    private String[] expenseCategories = {"food", "transport", "education", "parking", "travel", "shopping", "dessert", "entertainment"};
    private String[] incomeCategories = {"salary", "pocket_money", "part_time"};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        btnSave = findViewById(R.id.btnSave);
        categoryGrid = findViewById(R.id.categoryGrid);
        switchType = findViewById(R.id.switchType);

        mAuth = FirebaseAuth.getInstance();
        calendar = Calendar.getInstance();

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> addRecord());

        updateCategoryGrid(false);

        switchType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateCategoryGrid(isChecked);
            switchType.setText(isChecked ? "Income" : "Expense");
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateCategoryGrid(boolean isIncome) {
        categoryGrid.removeAllViews();

        String[] categories = isIncome ? incomeCategories : expenseCategories;

        for (String category : categories) {
            LinearLayout categoryLayout = new LinearLayout(this);
            categoryLayout.setOrientation(LinearLayout.VERTICAL);
            categoryLayout.setGravity(Gravity.CENTER);

            ImageButton categoryButton = new ImageButton(this);
            categoryButton.setContentDescription(category);
            categoryButton.setImageResource(getCategoryIcon(category));
            categoryButton.setBackgroundResource(R.drawable.icon_background);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.width = buttonParams.height = 200;
            categoryButton.setLayoutParams(buttonParams);

            TextView categoryName = new TextView(this);
            categoryName.setText(formatCategoryName(category));
            categoryName.setGravity(Gravity.CENTER);
            categoryName.setTextSize(12);

            categoryLayout.addView(categoryButton);
            categoryLayout.addView(categoryName);

            categoryButton.setOnClickListener(v -> {
                selectedCategory = category;
                updateCategoryButtonSelection(categoryLayout);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(8, 8, 8, 8);
            categoryLayout.setLayoutParams(params);

            categoryGrid.addView(categoryLayout);
        }
    }

    private String formatCategoryName(String category) {
        String[] words = category.split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase());
        }
        return formatted.toString();
    }

    private void updateCategoryButtonSelection(LinearLayout selectedLayout) {
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            LinearLayout layout = (LinearLayout) categoryGrid.getChildAt(i);
            ImageButton button = (ImageButton) layout.getChildAt(0);
            button.setBackgroundResource(R.drawable.icon_background);
            TextView text = (TextView) layout.getChildAt(1);
            text.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
        ImageButton selectedButton = (ImageButton) selectedLayout.getChildAt(0);
        selectedButton.setBackgroundResource(R.drawable.selected_category_background);
        TextView selectedText = (TextView) selectedLayout.getChildAt(1);
        selectedText.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
    }

    private int getCategoryIcon(String category) {
        switch (category) {
            case "food": return R.drawable.ic_food;
            case "transport": return R.drawable.ic_transport;
            case "education": return R.drawable.ic_education;
            case "parking": return R.drawable.ic_parking;
            case "travel": return R.drawable.ic_travel;
            case "shopping": return R.drawable.ic_shopping;
            case "dessert": return R.drawable.ic_dessert;
            case "entertainment": return R.drawable.ic_entertainment;
            case "salary": return R.drawable.ic_salary;
            case "pocket_money": return R.drawable.ic_pocketmoney;
            case "part_time": return R.drawable.ic_parttime;
            default: return R.drawable.ic_default;
        }
    }

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            etDate.setText(String.format("%04d-%02d-%02d", year1, month1 + 1, dayOfMonth));
        }, year, month, day);

        datePickerDialog.show();
    }

    private void addRecord() {
        String description = etDescription.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        boolean isIncome = switchType.isChecked();

        if (description.isEmpty() || amountStr.isEmpty() || date.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields and select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (!isIncome) {
            amount = -Math.abs(amount);
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        int year = Integer.parseInt(date.substring(0, 4));
        String month = date.substring(5, 7);

        String recordType = isIncome ? "incomes" : "expenses";
        String userId = currentUser.getUid();
        mDatabase = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(userId).child(recordType).child(String.valueOf(year)).child(month);

        String recordId = mDatabase.push().getKey();
        HashMap<String, Object> recordData = new HashMap<>();
        recordData.put("description", description);
        recordData.put("amount", amount);
        recordData.put("date", date);
        recordData.put("category", selectedCategory);

        if (recordId != null) {
            mDatabase.child(recordId).setValue(recordData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String message = isIncome ? "Income added" : "Expense added";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error adding record", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}