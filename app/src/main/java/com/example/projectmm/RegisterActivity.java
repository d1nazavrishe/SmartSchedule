package com.example.projectmm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPass, etConfirmPass;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etRegName);
        etPass = findViewById(R.id.etRegPass);
        etConfirmPass = findViewById(R.id.etRegConfirmPass);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String confirm = etConfirmPass.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Please enter a username");
                return;
            }
            if (pass.length() < 6) {
                etPass.setError("Password must be at least 6 characters");
                return;
            }
            if (!pass.equals(confirm)) {
                etConfirmPass.setError("Passwords do not match");
                return;
            }

            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("user_name", name);
            intent.putExtra("user_pass", pass);
            startActivity(intent);
            finish();
        });
    }
}
