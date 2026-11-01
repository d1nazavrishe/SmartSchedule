package com.example.projectmm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private String registeredUser = "";
    private String registeredPass = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etName = findViewById(R.id.etLoginName);
        EditText etPass = findViewById(R.id.etLoginPass);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvGoToRegister);

        if (getIntent().hasExtra("user_name")) {
            registeredUser = getIntent().getStringExtra("user_name");
            registeredPass = getIntent().getStringExtra("user_pass");
            etName.setText(registeredUser);
            Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
        }

        btnLogin.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (name.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.equals(registeredUser) && pass.equals(registeredPass) && !registeredUser.isEmpty()) {
                Toast.makeText(this, "Успешный вход!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Неверные данные или пользователь не создан", Toast.LENGTH_SHORT).show();
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}