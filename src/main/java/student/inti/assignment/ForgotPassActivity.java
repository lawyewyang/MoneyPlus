package student.inti.assignment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendResetEmail;
    private ImageButton btnBack;
    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_pass);

        etEmail = findViewById(R.id.etEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        btnBack = findViewById(R.id.btnBack);

        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPassActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSendResetEmail.setOnClickListener(v -> checkEmailAndSendResetLink());
    }

    private void checkEmailAndSendResetLink() {
        String userEmail = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail)) {
            Toast.makeText(this, "Please Enter Your Email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(userEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassActivity.this, "Reset password link sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = "Failed to send reset link.";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(ForgotPassActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
