package student.inti.assignment;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Calendar;
import java.util.HashMap;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class EditExpenseActivity extends AppCompatActivity {

    private TextInputEditText etDescription, etAmount, etDate;
    private MaterialButton btnSave, btnDelete;
    private GridLayout categoryGrid;
    private String selectedCategory = "";
    private SwitchMaterial switchType;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private Calendar calendar;
    private String recordId;
    private boolean isIncome;
    private String originalRecordType;

    private String[] expenseCategories = {"food", "transport", "education", "parking", "travel", "shopping", "dessert", "entertainment"};
    private String[] incomeCategories = {"salary", "pocket_money", "part_time"};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_expense);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Record");
        }

        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        categoryGrid = findViewById(R.id.categoryGrid);
        switchType = findViewById(R.id.switchType);

        mAuth = FirebaseAuth.getInstance();
        calendar = Calendar.getInstance();

        Intent intent = getIntent();
        etDescription.setText(intent.getStringExtra("description"));
        double amount = intent.getDoubleExtra("amount", 0.0);
        etAmount.setText(String.valueOf(Math.abs(amount)));
        etDate.setText(intent.getStringExtra("date"));
        selectedCategory = intent.getStringExtra("category");
        isIncome = amount >= 0;
        originalRecordType = isIncome ? "incomes" : "expenses";

        switchType.setChecked(isIncome);
        updateSwitchText(isIncome);

        recordId = intent.getStringExtra("expenseId");
        if (recordId == null) {
            recordId = intent.getStringExtra("incomeId");
        }

        if (recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "Error: recordId is missing", Toast.LENGTH_SHORT).show();
            Log.e("EditExpenseActivity", "No recordId received, unable to edit record.");
            finish();
            return;
        }

        updateCategoryGrid(isIncome);

        etDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveEditedRecord());
        btnDelete.setOnClickListener(v -> deleteRecord());

        switchType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isIncome = isChecked;
            updateSwitchText(isChecked);
            updateCategoryGrid(isIncome);
        });
    }

    private void updateSwitchText(boolean isIncome) {
        switchType.setText(isIncome ? "Income" : "Expense");
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
            categoryButton.setBackgroundResource(category.equals(selectedCategory) ? R.drawable.selected_category_background : R.drawable.icon_background);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.width = buttonParams.height = 200; // Adjust size as needed
            categoryButton.setLayoutParams(buttonParams);

            TextView categoryName = new TextView(this);
            categoryName.setText(formatCategoryName(category));
            categoryName.setGravity(Gravity.CENTER);
            categoryName.setTextSize(12); // Adjust text size as needed

            categoryLayout.addView(categoryButton);
            categoryLayout.addView(categoryName);

            categoryButton.setOnClickListener(v -> {
                selectedCategory = category;
                updateCategoryButtonSelection(categoryLayout);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(8, 8, 8, 8); // Add margins for spacing
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


    private void updateCategoryButtonSelection(ImageButton selectedButton) {
        for (int i = 0; i < categoryGrid.getChildCount(); i++) {
            ImageButton button = (ImageButton) categoryGrid.getChildAt(i);
            button.setBackgroundResource(R.drawable.icon_background);
        }
        selectedButton.setBackgroundResource(R.drawable.selected_category_background);
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

    private void saveEditedRecord() {
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

        if (recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "Error: recordId is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String newRecordType = isIncome ? "incomes" : "expenses";

        DatabaseReference oldRecordRef = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(userId).child(originalRecordType).child(String.valueOf(year)).child(month).child(recordId);

        DatabaseReference newRecordRef = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(userId).child(newRecordType).child(String.valueOf(year)).child(month).child(recordId);

        HashMap<String, Object> updatedData = new HashMap<>();
        updatedData.put("description", description);
        updatedData.put("amount", amount);
        updatedData.put("date", date);
        updatedData.put("category", selectedCategory);

        if (originalRecordType.equals(newRecordType)) {
            // Update existing record
            oldRecordRef.updateChildren(updatedData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("EditExpenseActivity", "Record updated successfully.");
                    Toast.makeText(this, "Record updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e("EditExpenseActivity", "Error updating record", task.getException());
                    Toast.makeText(this, "Error updating record", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Delete old record and create new one
            oldRecordRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("EditExpenseActivity", "Old record deleted successfully.");
                    newRecordRef.setValue(updatedData).addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            Log.d("EditExpenseActivity", "New record created successfully.");
                            Toast.makeText(this, "Record updated and moved", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Log.e("EditExpenseActivity", "Error creating new record", task2.getException());
                            Toast.makeText(this, "Error creating new record", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.e("EditExpenseActivity", "Error deleting old record", task.getException());
                    Toast.makeText(this, "Error deleting old record", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteRecord() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || recordId == null || recordId.isEmpty()) {
            Toast.makeText(this, "Error: User not authenticated or recordId is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String date = etDate.getText().toString();
        int year = Integer.parseInt(date.substring(0, 4));
        String month = date.substring(5, 7);

        DatabaseReference recordRef = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(userId).child(originalRecordType).child(String.valueOf(year)).child(month).child(recordId);

        // Remove the record from Firebase
        recordRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("EditExpenseActivity", "Record deleted successfully. Type: " + originalRecordType);
                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                finish();  // Go back to the previous screen
            } else {
                Log.e("EditExpenseActivity", "Error deleting record", task.getException());
                Toast.makeText(this, "Error deleting record", Toast.LENGTH_SHORT).show();
            }
        });
    }

}