package com.example.projectmm;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Экран 2: Список дел (To-Do List).
 * Позволяет:
 * - Просматривать все задачи и их параметры (длительность, подготовка, категория,
 *   приоритет, желаемое время суток, дедлайн, сколько раз в неделю нужно сделать).
 * - Нажимать на кнопку "+" и добавлять новую задачу со всеми параметрами.
 * - Прямо с карточки поставить задачу в расписание ("+ Schedule") или удалить её.
 */
public class TodoFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private LinearLayout llTodoList;
    private TextView tvEmptyTodo;
    private TextView tvTodoSummary;
    private FloatingActionButton fabAddTodo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        llTodoList = view.findViewById(R.id.llTodoList);
        tvEmptyTodo = view.findViewById(R.id.tvEmptyTodo);
        tvTodoSummary = view.findViewById(R.id.tvTodoSummary);
        fabAddTodo = view.findViewById(R.id.fabAddTodo);

        // Нажатие на плюсик открывает диалог создания новой задачи
        fabAddTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTodoDialog();
            }
        });

        refreshTodoList();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshTodoList();
    }

    /**
     * Загружает все задачи из SQLite и отображает их карточками на экране.
     */
    private void refreshTodoList() {
        llTodoList.removeAllViews();
        List<TodoItem> items = dbHelper.getAllTodoItems();

        if (items.isEmpty()) {
            tvEmptyTodo.setVisibility(View.VISIBLE);
            tvTodoSummary.setText("0 tasks · Tap + to add tasks");
            return;
        }

        tvEmptyTodo.setVisibility(View.GONE);
        int remainingCount = dbHelper.getRemainingTodoItems().size();
        tvTodoSummary.setText(items.size() + " tasks · " + remainingCount + " waiting to be scheduled");

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (final TodoItem item : items) {
            View card = inflater.inflate(R.layout.item_todo_card, llTodoList, false);

            View strip = card.findViewById(R.id.viewCategoryStrip);
            TextView tvCategory = card.findViewById(R.id.tvTodoCategory);
            TextView tvPriority = card.findViewById(R.id.tvTodoPriority);
            TextView tvTitle = card.findViewById(R.id.tvTodoTitle);
            TextView tvDetails = card.findViewById(R.id.tvTodoDetails);
            TextView tvRepeatInfo = card.findViewById(R.id.tvTodoRepeatInfo);
            ImageButton btnDelete = card.findViewById(R.id.btnDeleteTodo);
            MaterialButton btnQuickSchedule = card.findViewById(R.id.btnQuickSchedule);

            // Раскрашиваем полоску и бейдж по категории задачи
            int neonColor = ContextCompat.getColor(requireContext(),
                    ScheduleEvent.getCategoryNeonColorRes(item.getCategory()));
            int pastelColor = ContextCompat.getColor(requireContext(),
                    ScheduleEvent.getCategoryPastelColorRes(item.getCategory()));

            strip.setBackgroundColor(neonColor);
            tvCategory.setText(item.getCategory());
            tvCategory.setBackgroundColor(pastelColor);
            tvCategory.setTextColor(neonColor);

            tvPriority.setText("Priority: " + item.getPriority());
            tvTitle.setText(item.getTitle());

            // Собираем строку параметров: длительность, подготовка, время суток, дедлайн
            StringBuilder details = new StringBuilder();
            details.append("Duration: ").append(ScheduleEvent.formatDuration(item.getDurationMinutes()));
            if (item.getPrepMinutes() > 0) {
                details.append(" · Prep: +").append(item.getPrepMinutes()).append("m");
            }
            if (!"Any".equalsIgnoreCase(item.getPreferredTime())) {
                details.append(" · ").append(item.getPreferredTime());
            }
            if (item.getDeadlineDay() >= 0) {
                details.append(" · Due: ").append(ScheduleEvent.getDayShort(item.getDeadlineDay()));
            }
            tvDetails.setText(details.toString());

            // Считаем, сколько раз задача уже добавлена в расписание
            int scheduledCount = dbHelper.getScheduledCountForTodo(item.getId());
            if (TodoItem.REPEAT_WEEKLY_FIXED.equals(item.getRepeatType())) {
                tvRepeatInfo.setText("Weekly: " + ScheduleEvent.getDayShort(item.getFixedDay())
                        + " at " + ScheduleEvent.formatTime(item.getFixedStartMinutes()));
            } else if (TodoItem.REPEAT_TIMES_PER_WEEK.equals(item.getRepeatType())) {
                tvRepeatInfo.setText(item.getTargetCount() + "x / week · Scheduled: "
                        + scheduledCount + "/" + item.getTargetCount());
            } else {
                tvRepeatInfo.setText("Once · Scheduled: " + scheduledCount + "/1");
            }

            // Если уже запланировано нужное число раз, меняем цвет текста на зелёный
            if (scheduledCount >= item.getTargetCount()) {
                tvRepeatInfo.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
                btnQuickSchedule.setText("+ Add Again");
            } else {
                tvRepeatInfo.setTextColor(neonColor);
                btnQuickSchedule.setText("+ Schedule");
            }

            // Кнопка быстрого добавления этой задачи в расписание на выбранный день и час
            btnQuickSchedule.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showQuickScheduleDialog(item);
                }
            });

            // Кнопка удаления задачи из To-Do
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.deleteTodoItem(item.getId());
                    refreshTodoList();
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                }
            });

            llTodoList.addView(card);
        }
    }

    /**
     * Диалог добавления новой задачи в To-Do List со всеми параметрами.
     */
    private void showAddTodoDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_todo, null);

        final EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        final EditText etDuration = dialogView.findViewById(R.id.etDurationMin);
        final EditText etPrep = dialogView.findViewById(R.id.etPrepMin);
        final Spinner spCategory = dialogView.findViewById(R.id.spCategory);
        final Spinner spPriority = dialogView.findViewById(R.id.spPriority);
        final Spinner spPrefTime = dialogView.findViewById(R.id.spPreferredTime);
        final Spinner spDeadline = dialogView.findViewById(R.id.spDeadline);
        final Spinner spRepeatMode = dialogView.findViewById(R.id.spRepeatMode);
        final LinearLayout llTimesPerWeek = dialogView.findViewById(R.id.llTimesPerWeek);
        final EditText etTimesCount = dialogView.findViewById(R.id.etTimesCount);
        final LinearLayout llWeeklyFixed = dialogView.findViewById(R.id.llWeeklyFixed);
        final Spinner spFixedDay = dialogView.findViewById(R.id.spFixedDay);
        final Button btnPickFixedTime = dialogView.findViewById(R.id.btnPickFixedTime);

        // Настраиваем выпадающие списки (Spinner)
        spCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ScheduleEvent.CATEGORIES));

        spPriority.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ScheduleEvent.PRIORITIES));
        spPriority.setSelection(1); // Medium по умолчанию

        spPrefTime.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ScheduleEvent.PREFERRED_TIMES));

        String[] deadlineOptions = {
                "No deadline", "Sunday", "Monday", "Tuesday",
                "Wednesday", "Thursday", "Friday", "Saturday"
        };
        spDeadline.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, deadlineOptions));

        String[] repeatOptions = {
                "Once (1 time)",
                "Multiple times per week (2x, 3x...)",
                "Every week at a specific time"
        };
        spRepeatMode.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, repeatOptions));

        spFixedDay.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ScheduleEvent.DAYS_FULL));

        // Храним выбранное фиксированное время в массиве из 1 элемента (по умолчанию 16:00 = 960 мин)
        final int[] fixedStartMin = {16 * 60};
        btnPickFixedTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = fixedStartMin[0] / 60;
                int min = fixedStartMin[0] % 60;
                new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        fixedStartMin[0] = hourOfDay * 60 + minute;
                        btnPickFixedTime.setText(ScheduleEvent.formatTime(fixedStartMin[0]));
                    }
                }, hour, min, true).show();
            }
        });

        // Показываем дополнительные поля в зависимости от выбранного режима повторения
        spRepeatMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                llTimesPerWeek.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                llWeeklyFixed.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("New To-Do Task")
                .setView(dialogView)
                .setPositiveButton("Add Task", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String title = etTitle.getText().toString().trim();
                        if (title.isEmpty()) {
                            Toast.makeText(requireContext(), "Please enter a task title", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int duration = parsePositiveInt(etDuration.getText().toString(), 60);
                        int prep = parseNonNegativeInt(etPrep.getText().toString(), 0);
                        String category = ScheduleEvent.CATEGORIES[spCategory.getSelectedItemPosition()];
                        String priority = ScheduleEvent.PRIORITIES[spPriority.getSelectedItemPosition()];
                        String prefTime = ScheduleEvent.PREFERRED_TIMES[spPrefTime.getSelectedItemPosition()];
                        int deadlineDay = spDeadline.getSelectedItemPosition() - 1; // -1 = No deadline, 0..6 = Sun..Sat

                        int repeatIndex = spRepeatMode.getSelectedItemPosition();
                        String repeatType = TodoItem.REPEAT_ONCE;
                        int targetCount = 1;
                        int fixedDay = -1;
                        int fixedTime = -1;

                        if (repeatIndex == 1) {
                            repeatType = TodoItem.REPEAT_TIMES_PER_WEEK;
                            targetCount = Math.max(2, parsePositiveInt(etTimesCount.getText().toString(), 2));
                        } else if (repeatIndex == 2) {
                            repeatType = TodoItem.REPEAT_WEEKLY_FIXED;
                            targetCount = 1;
                            fixedDay = spFixedDay.getSelectedItemPosition();
                            fixedTime = fixedStartMin[0];
                        }

                        TodoItem newItem = new TodoItem(
                                title, duration, prep, category, priority, prefTime,
                                deadlineDay, repeatType, targetCount, fixedDay, fixedTime
                        );
                        dbHelper.addTodoItem(newItem);
                        refreshTodoList();

                        if (TodoItem.REPEAT_WEEKLY_FIXED.equals(repeatType)) {
                            Toast.makeText(requireContext(),
                                    "Added to To-Do & Weekly Schedule!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Task added to To-Do List!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Диалог выбора дня недели и времени, чтобы быстро поставить задачу из To-Do в расписание.
     */
    private void showQuickScheduleDialog(final TodoItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Schedule \"" + item.getTitle() + "\"")
                .setItems(ScheduleEvent.DAYS_FULL, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int selectedDay) {
                        // Автоматически предлагаем первое свободное окно в выбранный день
                        int suggestedStart = dbHelper.findFirstFreeSlot(
                                selectedDay, item.getDurationMinutes(), item.getPrepMinutes());

                        new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                int startMinutes = hourOfDay * 60 + minute;
                                ScheduleEvent ev = new ScheduleEvent(
                                        item.getTitle(),
                                        selectedDay,
                                        startMinutes,
                                        item.getDurationMinutes(),
                                        item.getPrepMinutes(),
                                        item.getCategory(),
                                        item.getId(),
                                        false
                                );
                                dbHelper.addScheduleEvent(ev);
                                refreshTodoList();
                                Toast.makeText(requireContext(),
                                        "Scheduled on " + ScheduleEvent.getDayShort(selectedDay)
                                                + " at " + ScheduleEvent.formatTime(startMinutes),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, suggestedStart / 60, suggestedStart % 60, true).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int parsePositiveInt(String text, int fallback) {
        try {
            int val = Integer.parseInt(text.trim());
            return val > 0 ? val : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parseNonNegativeInt(String text, int fallback) {
        try {
            int val = Integer.parseInt(text.trim());
            return Math.max(0, val);
        } catch (Exception e) {
            return fallback;
        }
    }
}
