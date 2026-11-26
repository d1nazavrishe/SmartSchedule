package com.example.projectmm;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Главный экран приложения после авторизации.
 * Содержит нижнюю панель навигации (BottomNavigationView) и переключает три экрана:
 * 1) ScheduleFragment — расписание по дням недели и часам + загруженность дня.
 * 2) TodoFragment — список задач (To-Do List) со всеми параметрами.
 * 3) AiChatFragment — общение с ИИ-помощником (Gemini API).
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        // По умолчанию при первом открытии показываем экран расписания (Schedule)
        if (savedInstanceState == null) {
            loadFragment(new ScheduleFragment());
        }

        // Обработка нажатий на вкладки нижней панели
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_schedule) {
                    loadFragment(new ScheduleFragment());
                    return true;
                } else if (id == R.id.nav_todo) {
                    loadFragment(new TodoFragment());
                    return true;
                } else if (id == R.id.nav_ai) {
                    loadFragment(new AiChatFragment());
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Заменяет текущий фрагмент в контейнере fragmentContainer на выбранный.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
