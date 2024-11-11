package student.inti.assignment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegUsername, etRegEmail, etRegPassword, etRegConfirmPassword;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance(); // 获取Firebase Auth实例
        databaseReference = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users"); // 获取Database引用

        etRegUsername = findViewById(R.id.etRegUsername);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etRegUsername.getText().toString().trim();
                String email = etRegEmail.getText().toString().trim();
                String password = etRegPassword.getText().toString().trim();
                String confirmPassword = etRegConfirmPassword.getText().toString().trim();

                if (validateInputs(username, email, password, confirmPassword)) {
                    registerUser(username, email, password);
                }
            }
        });
    }

    // 验证输入框
    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            etRegUsername.setError("Please enter username");
            etRegUsername.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Please enter a valid email");
            etRegEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6 || password.length() > 12 || !password.matches(".*[a-zA-Z].*")) {
            etRegPassword.setError("Passwords must be 6-12 digits long and contain a letter or punctuation mark.");
            etRegPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etRegConfirmPassword.setError("Passwords do not match");
            etRegConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    // 注册用户
    private void registerUser(final String username, final String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null) {
                    String userId = firebaseUser.getUid(); // 获取用户ID
                    saveUserToDatabase(userId, username, email);
                    Toast.makeText(RegisterActivity.this, "Register Complete", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                }
            } else {
                Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 将用户信息保存到Firebase Database
    private void saveUserToDatabase(String userId, String username, String email) {
        User user = new User(username, email);
        databaseReference.child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this, "User data saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RegisterActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 定义一个用户类
    public static class User {
        public String username;
        public String email;

        public User(String username, String email) {
            this.username = username;
            this.email = email;
        }
    }
}
