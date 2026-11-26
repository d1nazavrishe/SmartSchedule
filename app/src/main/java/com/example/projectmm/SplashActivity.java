package com.example.projectmm;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;

    private MaterialCardView cardLogo;
    private ImageView imgLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();

        // Загружаем и запускаем анимацию появления логотипа
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.splash_anim);
        cardLogo.startAnimation(anim);

        // Поток ждёт 3 секунды и переходит на экран входа (LoginActivity)
        Thread splashThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(SPLASH_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        splashThread.start();
    }

    private void initViews() {
        cardLogo = findViewById(R.id.cardLogo);
        imgLogo = findViewById(R.id.imgLogo);
    }
}
