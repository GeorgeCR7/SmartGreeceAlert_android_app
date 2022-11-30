package com.example.smartgreecealert.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.example.smartgreecealert.R;
import com.example.smartgreecealert.helpers.Language;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private EditText email, password;
    private Language languageHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languageHelper = new Language(this);
        languageHelper.loadLocale();
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBarLogin);

        email = findViewById(R.id.editTextTextEmailAddressLogin);
        password = findViewById(R.id.editTextTextPasswordLogin);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If the user is logged in, redirect them to main menu
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Login to Firebase and redirect user to main menu
     *
     * @param view View
     */
    public void login(View view) {
        String emailText = email.getText().toString();
        String passwordText = password.getText().toString();

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(LoginActivity.this, R.string.loginEmpty,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(LoginActivity.this, Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Changes application's language
     *
     * @param view View
     */
    public void changeLanguage(View view) {
        languageHelper.showChangeLanguageDialog();
    }

    /**
     * Go to register form
     *
     * @param view View
     */
    public void goToRegister(View view) {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}