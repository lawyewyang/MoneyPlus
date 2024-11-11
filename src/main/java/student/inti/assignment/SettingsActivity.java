package student.inti.assignment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvNickname, tvGender;
    private ImageView ivProfilePicture;
    private Button btnChangeNickname, btnChangeGender, btnChangeProfilePicture, btnLogout;
    private String userNodeId;
    private static final String TAG = "SettingsActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        tvNickname = findViewById(R.id.tvNickname);
        tvGender = findViewById(R.id.tvGender);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnChangeNickname = findViewById(R.id.btnChangeNickname);
        btnChangeGender = findViewById(R.id.btnChangeGender);
        btnChangeProfilePicture = findViewById(R.id.btnChangeProfilePicture);
        btnLogout = findViewById(R.id.btnLogout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get current user ID from Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Use current user's UID to get data from Firebase
            userNodeId = currentUser.getUid();
            databaseRef = FirebaseDatabase.getInstance("https://moneyplus-fb568-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("users");

            loadUserData();

            // Set up listeners for user data changes
            btnChangeNickname.setOnClickListener(v -> showChangeNicknameDialog());
            btnChangeGender.setOnClickListener(v -> showChangeGenderDialog());
            btnChangeProfilePicture.setOnClickListener(v -> openFileChooser());

            // Log Out button functionality
            btnLogout.setOnClickListener(v -> {
                // Log out from Firebase Authentication
                FirebaseAuth.getInstance().signOut();

                // Return to LoginActivity
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();  // Close current activity
            });
        } else {
            // Handle case when user is not logged in
            Toast.makeText(SettingsActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();  // Optionally, close the activity
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // When the back button is pressed, navigate to HomeActivity
            Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // This clears the back stack
            startActivity(intent);
            finish(); // Optionally, finish SettingsActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserData() {
        // Retrieve user data using the current user's UID
        databaseRef.child(userNodeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nickname = dataSnapshot.child("nickname").getValue(String.class);
                    String gender = dataSnapshot.child("gender").getValue(String.class);
                    String encodedImage = dataSnapshot.child("profilePicture").getValue(String.class);

                    tvNickname.setText(nickname != null ? nickname : "Not set");
                    tvGender.setText(gender != null ? gender : "Not set");

                    if (encodedImage != null) {
                        Bitmap profileImage = decodeBase64ToImage(encodedImage);
                        ivProfilePicture.setImageBitmap(profileImage);
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SettingsActivity.this, "Error loading data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangeNicknameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Nickname");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newNickname = input.getText().toString();
            if (!newNickname.isEmpty()) {
                updateUserField("nickname", newNickname);
            } else {
                Toast.makeText(SettingsActivity.this, "Nickname cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showChangeGenderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Gender");

        View view = getLayoutInflater().inflate(R.layout.dialog_gender, null);
        builder.setView(view);

        RadioGroup radioGroup = view.findViewById(R.id.rgGender);
        RadioButton rbMale = view.findViewById(R.id.rbMale);
        RadioButton rbFemale = view.findViewById(R.id.rbFemale);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == rbMale.getId()) {
                updateUserField("gender", "Male");
            } else if (selectedId == rbFemale.getId()) {
                updateUserField("gender", "Female");
            } else {
                Toast.makeText(SettingsActivity.this, "Please select a gender", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ivProfilePicture.setImageBitmap(bitmap); // 显示图片
                String encodedImage = encodeImageToBase64(bitmap); // 转换为Base64字符串
                uploadProfilePicture(encodedImage); // 上传Base64编码的图像
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void uploadProfilePicture(String encodedImage) {
        if (userNodeId != null && encodedImage != null) {
            databaseRef.child(userNodeId).child("profilePicture").setValue(encodedImage)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SettingsActivity.this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Profile picture updated in database.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating profile picture", e);
                        Toast.makeText(SettingsActivity.this, "Error updating profile picture", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e(TAG, "Invalid image or userNodeId is null");
            Toast.makeText(SettingsActivity.this, "Invalid image or user ID", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeBase64ToImage(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void updateUserField(String field, String value) {
        if (userNodeId != null) {
            databaseRef.child(userNodeId).child(field).setValue(value)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SettingsActivity.this, field + " updated successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, field + " updated in database.");
                        loadUserData(); // Reload the updated data
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating " + field, e);
                        Toast.makeText(SettingsActivity.this, "Error updating " + field, Toast.LENGTH_SHORT).show();
                    });
        }
    }
}