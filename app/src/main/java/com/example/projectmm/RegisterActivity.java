package com.example.projectmm;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });

        tvGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void signUp() {
        String email = String.valueOf(etEmail.getText()).trim();
        String password = String.valueOf(etPassword.getText());
        String confirm = String.valueOf(etConfirmPassword.getText());

        if (!isValid(email, password, confirm)) {
            return;
        }

        Toast.makeText(this, R.string.msg_signup_success, Toast.LENGTH_SHORT).show();

        // Передаём данные на экран входа через Intent extras (Урок 1)
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.putExtra("username", email);
        intent.putExtra("password", password);
        startActivity(intent);
        finish();
    }

    private boolean isValid(String email, String password, String confirm) {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        boolean valid = true;

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.err_empty_email));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.err_invalid_email));
            valid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.err_empty_password));
            valid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError(getString(R.string.err_short_password));
            valid = false;
        }

        if (!confirm.equals(password)) {
            tilConfirmPassword.setError(getString(R.string.err_password_mismatch));
            valid = false;
        }

        return valid;
    }
}
