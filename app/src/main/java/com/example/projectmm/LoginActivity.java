package com.example.projectmm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember_me";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialCheckBox cbRememberMe;
    private MaterialButton btnLogin;
    private TextView tvToRegister;

    private SharedPreferences prefs;

    // Данные, полученные с экрана регистрации через Intent extras (Урок 1)
    private String registeredEmail;
    private String registeredPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();

        // Урок 2: открываем SharedPreferences и загружаем сохранённые данные
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadCredentials();

        // Урок 1: проверяем, пришли ли данные с экрана регистрации через Intent
        Intent intent = getIntent();
        if (intent != null) {
            String usernameExtra = intent.getStringExtra("username");
            String passwordExtra = intent.getStringExtra("password");
            if (usernameExtra != null && passwordExtra != null) {
                registeredEmail = usernameExtra;
                registeredPassword = passwordExtra;
                etEmail.setText(registeredEmail);
            }
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        tvToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        cbRememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked && prefs.getBoolean(KEY_REMEMBER, false)) {
                    showForgetSnackbar();
                }
            }
        });
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        tvToRegister = findViewById(R.id.tvToRegister);
    }

    private void signIn() {
        String email = String.valueOf(etEmail.getText()).trim();
        String password = String.valueOf(etPassword.getText());

        if (!isValid(email, password)) {
            return;
        }

        // Если с экрана регистрации переданы данные (Урок 1), проверяем совпадение через equals()
        if (registeredEmail != null && registeredPassword != null) {
            if (!email.equals(registeredEmail) || !password.equals(registeredPassword)) {
                Toast.makeText(this, R.string.err_wrong_credentials, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Урок 2: если отмечен чекбокс «Запомни меня», сохраняем в SharedPreferences
        if (cbRememberMe.isChecked()) {
            saveCredentials(email, password);
        }

        Toast.makeText(this, R.string.msg_signin_success, Toast.LENGTH_SHORT).show();

        // После успешного входа открываем главный экран с расписанием, To-Do и ИИ
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private boolean isValid(String email, String password) {
        tilEmail.setError(null);
        tilPassword.setError(null);
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
        }

        return valid;
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER, true);
        editor.apply();
    }

    private void loadCredentials() {
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);

        if (remember) {
            etEmail.setText(prefs.getString(KEY_EMAIL, ""));
            etPassword.setText(prefs.getString(KEY_PASSWORD, ""));
            cbRememberMe.setChecked(true);
        }
    }

    private void showForgetSnackbar() {
        Snackbar snackbar = Snackbar.make(cbRememberMe, R.string.forget_saved_prompt, Snackbar.LENGTH_LONG);

        snackbar.setAction(R.string.forget_saved_action, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearCredentials();
            }
        });

        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar sb, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                    cbRememberMe.setChecked(true);
                }
            }
        });

        snackbar.show();
    }

    private void clearCredentials() {
        prefs.edit().clear().apply();
    }
}
