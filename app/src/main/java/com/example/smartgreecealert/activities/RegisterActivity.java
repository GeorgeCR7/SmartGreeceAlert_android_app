package com.example.smartgreecealert.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.smartgreecealert.R;
import com.example.smartgreecealert.models.Register;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private EditText fistName, lastName, email, password, retypePassword;
    private final String TAG = "register";
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");

        fistName = findViewById(R.id.editTextTextPersonFirstNameRegister);
        lastName = findViewById(R.id.editTextTextPersonLastNameRegister);
        email = findViewById(R.id.editTextTextEmailAddressRegister);
        password = findViewById(R.id.editTextTextPasswordRegister);
        retypePassword = findViewById(R.id.editTextTextPassword2Register);
        progressBar = findViewById(R.id.progressBarRegister);
    }

    /**
     * This method registers user to Firebase
     *
     * @param view View
     */
    public void register(View view) {
        String fistNameText = fistName.getText().toString();
        String lastNameText = lastName.getText().toString();
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();
        String retypePasswordText = retypePassword.getText().toString();

        Register register = new Register.Builder()
                .withFullName(fistNameText, lastNameText)
                .withEmail(emailText)
                .withPassword(passwordText, retypePasswordText)
                .build();

        // Validate form inputs
        if (!validateFields(register)) {
            return;
        }

        // Firebase method to create a user
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(register.getEmail(), register.getPassword())
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        userRef.child(user.getUid()).child("firstName").setValue(register.getFistName());
                        userRef.child(user.getUid()).child("lastName").setValue(register.getLastName());
                        Toast.makeText(RegisterActivity.this, R.string.registerSuccess,
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainMenuActivity.class);
                        startActivity(intent);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, R.string.registerFailed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * This method validates form inputs
     *
     * @param register Register
     * @return boolean
     */
    public boolean validateFields(Register register) {
        if (register.getFistName().equals("") ||
                register.getLastName().equals("") ||
                register.getEmail().equals("") ||
                register.getPassword().equals("") ||
                register.getRetypePassword().equals("")) {
            Toast.makeText(RegisterActivity.this, R.string.fillAllFields, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!isValidEmail(register.getEmail())) {
            Toast.makeText(RegisterActivity.this, R.string.invalidEmail, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (register.getPassword().length() < 6) {
            Toast.makeText(RegisterActivity.this, R.string.invalidPassword, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!register.getPassword().equals(register.getRetypePassword())) {
            Toast.makeText(RegisterActivity.this, R.string.passwordsNotMatch, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Checks if email is valid
     *
     * @param target CharSequence
     * @return boolean
     */
    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}