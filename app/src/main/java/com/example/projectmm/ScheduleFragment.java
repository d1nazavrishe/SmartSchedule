package com.example.projectmm;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Экран 1: Почасовое расписание на неделю (Schedule).
 * Возможности:
 * - Сверху переключение дня недели (начиная с воскресенья: Sun, Mon, Tue, Wed, Thu, Fri, Sat).
 * - Отображение общей загруженности выбранного дня в процентах и часах с цветовой индикацией.
 * - Почасовая лента с 07:00 до 23:00 (с учётом времени подготовки и продолжающихся дел).
 * - Добавление событий напрямую ИЛИ выбор готовой задачи из списка To-Do List.
 * - По нажатию на событие его можно перенести на другой день/время или удалить.
 */
public class ScheduleFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private LinearLayout llDaySelector;
    private TextView tvSelectedDayTitle;
    private TextView tvWorkloadBadge;
    private ProgressBar pbWorkload;
    private TextView tvWorkloadDetails;
    private MaterialButton btnAddFromTodo;
    private MaterialButton btnAddCustomEvent;
    private LinearLayout llHourlyTimeline;

    // Текущий выбранный день недели (0 = Sunday ... 6 = Saturday)
    private int selectedDay = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        llDaySelector = view.findViewById(R.id.llDaySelector);
        tvSelectedDayTitle = view.findViewById(R.id.tvSelectedDayTitle);
        tvWorkloadBadge = view.findViewById(R.id.tvWorkloadBadge);
        pbWorkload = view.findViewById(R.id.pbWorkload);
        tvWorkloadDetails = view.findViewById(R.id.tvWorkloadDetails);
        btnAddFromTodo = view.findViewById(R.id.btnAddFromTodo);
        btnAddCustomEvent = view.findViewById(R.id.btnAddCustomEvent);
        llHourlyTimeline = view.findViewById(R.id.llHourlyTimeline);

        // По умолчанию выбираем сегодняшний день недели (в Calendar SUNDAY=1 -> индекс 0)
        int calendarDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        selectedDay = calendarDay - 1;

        // Кнопка добавления задачи из To-Do List
        btnAddFromTodo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPickFromTodoDialog(null);
            }
        });

        // Кнопка добавления нового события вручную
        btnAddCustomEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCustomEventDialog(9 * 60);
            }
        });

        buildDaySelectorButtons();
        refreshDaySchedule();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        buildDaySelectorButtons();
        refreshDaySchedule();
    }

    /**
     * Строит 7 кнопок дней недели сверху (Sun..Sat) и подсвечивает активный день.
     */
    private void buildDaySelectorButtons() {
        llDaySelector.removeAllViews();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            TextView tvDay = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, (int) (38 * density), 1f);
            if (i < 6) {
                params.setMarginEnd((int) (4 * density));
            }
            tvDay.setLayoutParams(params);
            tvDay.setGravity(Gravity.CENTER);
            tvDay.setText(ScheduleEvent.DAYS_SHORT[i]);
            tvDay.setTextSize(13f);
            tvDay.setTypeface(null, Typeface.BOLD);

            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(18 * density);

            if (dayIndex == selectedDay) {
                // Выбранный день подсвечивается ярко-розовым цветом
                pill.setColor(ContextCompat.getColor(requireContext(), R.color.pink_neon));
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface));
            } else {
                // Остальные дни полупрозрачные
                pill.setColor(0x33FFFFFF);
                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface));
            }
            tvDay.setBackground(pill);

            tvDay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDay = dayIndex;
                    buildDaySelectorButtons();
                    refreshDaySchedule();
                }
            });

            llDaySelector.addView(tvDay);
        }
    }

    /**
     * Обновляет карточку загруженности дня и перерисовывает почасовую шкалу (07:00 - 23:00).
     */
    private void refreshDaySchedule() {
        tvSelectedDayTitle.setText(ScheduleEvent.getDayFull(selectedDay));

        // 1. Считаем общую загруженность дня
        int busyMinutes = dbHelper.getDayBusyMinutes(selectedDay);
        int workloadPercent = dbHelper.getDayWorkloadPercent(selectedDay);
        pbWorkload.setProgress(workloadPercent);
        tvWorkloadDetails.setText(workloadPercent + "% Daily Workload · "
                + ScheduleEvent.formatDuration(busyMinutes) + " busy (incl. prep time)");

        // Выбираем цвет индикатора загруженности: зелёный / фиолетовый / ярко-розовый
        int badgeTextColor;
        int badgeBgColor;
        String badgeLabel;
        if (workloadPercent <= 45) {
            badgeLabel = "Light load";
            badgeTextColor = ContextCompat.getColor(requireContext(), R.color.green);
            badgeBgColor = ContextCompat.getColor(requireContext(), R.color.green_pastel);
        } else if (workloadPercent <= 75) {
            badgeLabel = "Balanced";
            badgeTextColor = ContextCompat.getColor(requireContext(), R.color.purple);
            badgeBgColor = ContextCompat.getColor(requireContext(), R.color.purple_pastel);
        } else {
            badgeLabel = "Heavy load!";
            badgeTextColor = ContextCompat.getColor(requireContext(), R.color.pink);
            badgeBgColor = ContextCompat.getColor(requireContext(), R.color.pink_pastel);
        }

        tvWorkloadBadge.setText(badgeLabel);
        tvWorkloadBadge.setTextColor(badgeTextColor);
        tvWorkloadBadge.setBackgroundColor(badgeBgColor);
        pbWorkload.setProgressTintList(ColorStateList.valueOf(badgeTextColor));

        // 2. Строим почасовую ленту с 07:00 до 23:00
        llHourlyTimeline.removeAllViews();
        List<ScheduleEvent> dayEvents = dbHelper.getEventsForDay(selectedDay);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int hour = 7; hour <= 23; hour++) {
            final int currentHour = hour;
            int hourStart = hour * 60;
            int hourEnd = (hour + 1) * 60;

            View hourRow = inflater.inflate(R.layout.item_hour_row, llHourlyTimeline, false);
            TextView tvHourLabel = hourRow.findViewById(R.id.tvHourLabel);
            LinearLayout llHourEvents = hourRow.findViewById(R.id.llHourEvents);

            tvHourLabel.setText(ScheduleEvent.formatTime(hourStart));

            // Ищем все события, которые начинаются в этот час (или раньше 07:00 для первой строки)
            List<ScheduleEvent> startingHere = new ArrayList<>();
            ScheduleEvent ongoingEvent = null;

            for (ScheduleEvent ev : dayEvents) {
                if ((hour == 7 && ev.getStartMinutes() < hourEnd)
                        || (ev.getStartMinutes() >= hourStart && ev.getStartMinutes() < hourEnd)) {
                    startingHere.add(ev);
                } else if (ev.getEffectiveStartMinutes() < hourEnd && ev.getEndMinutes() > hourStart
                        && ev.getStartMinutes() < hourStart) {
                    ongoingEvent = ev;
                }
            }

            if (!startingHere.isEmpty()) {
                // Отображаем карточки событий, начинающихся в этом часе
                for (final ScheduleEvent ev : startingHere) {
                    View eventCard = createEventCardView(inflater, llHourEvents, ev);
                    llHourEvents.addView(eventCard);
                }
            } else if (ongoingEvent != null) {
                // Показываем аккуратную пометку, что в этом часе продолжается длинное событие
                TextView tvCont = new TextView(requireContext());
                tvCont.setText("↳ Continuing: " + ongoingEvent.getTitle()
                        + " (until " + ScheduleEvent.formatTime(ongoingEvent.getEndMinutes()) + ")");
                tvCont.setTextSize(12f);
                tvCont.setTextColor(ContextCompat.getColor(requireContext(),
                        ScheduleEvent.getCategoryNeonColorRes(ongoingEvent.getCategory())));
                tvCont.setPadding(16, 10, 16, 10);
                tvCont.setBackgroundColor(ContextCompat.getColor(requireContext(),
                        ScheduleEvent.getCategoryPastelColorRes(ongoingEvent.getCategory())));
                llHourEvents.addView(tvCont);
            } else {
                // Свободный час — можно нажать прямо на строку, чтобы поставить сюда дело!
                TextView tvFree = new TextView(requireContext());
                tvFree.setText("+ Free slot (tap to schedule)");
                tvFree.setTextSize(12f);
                tvFree.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                tvFree.setPadding(8, 12, 8, 12);
                tvFree.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSlotChoiceDialog(currentHour * 60);
                    }
                });
                llHourEvents.addView(tvFree);
            }

            llHourlyTimeline.addView(hourRow);
        }
    }

    /**
     * Создаёт красивую цветную карточку события для почасовой шкалы.
     */
    private View createEventCardView(LayoutInflater inflater, ViewGroup parent, final ScheduleEvent ev) {
        View cardView = inflater.inflate(R.layout.item_schedule_event, parent, false);

        MaterialCardView card = cardView.findViewById(R.id.cardEvent);
        View strip = cardView.findViewById(R.id.viewEventStrip);
        TextView tvPrepBanner = cardView.findViewById(R.id.tvPrepBanner);
        TextView tvTimeRange = cardView.findViewById(R.id.tvEventTimeRange);
        TextView tvCategory = cardView.findViewById(R.id.tvEventCategory);
        TextView tvTitle = cardView.findViewById(R.id.tvEventTitle);

        int pastel = ContextCompat.getColor(requireContext(),
                ScheduleEvent.getCategoryPastelColorRes(ev.getCategory()));
        int neon = ContextCompat.getColor(requireContext(),
                ScheduleEvent.getCategoryNeonColorRes(ev.getCategory()));

        card.setCardBackgroundColor(pastel);
        strip.setBackgroundColor(neon);
        tvTimeRange.setTextColor(neon);
        tvCategory.setTextColor(neon);

        // Если указано время подготовки/дороги — отображаем его сверху карточки
        if (ev.getPrepMinutes() > 0) {
            tvPrepBanner.setVisibility(View.VISIBLE);
            tvPrepBanner.setText("Prep / Travel: "
                    + ScheduleEvent.formatTime(ev.getEffectiveStartMinutes())
                    + " - " + ScheduleEvent.formatTime(ev.getStartMinutes())
                    + " (" + ev.getPrepMinutes() + "m)");
        } else {
            tvPrepBanner.setVisibility(View.GONE);
        }

        String timeInfo = ScheduleEvent.formatTime(ev.getStartMinutes()) + " - "
                + ScheduleEvent.formatTime(ev.getEndMinutes())
                + " (" + ScheduleEvent.formatDuration(ev.getDurationMinutes()) + ")";
        if (ev.isWeekly()) {
            timeInfo += " · Weekly";
        }
        tvTimeRange.setText(timeInfo);
        tvCategory.setText(ev.getCategory());
        tvTitle.setText(ev.getTitle());

        // По нажатию на карточку открываем меню управления событием (перенести / удалить)
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEventActionsDialog(ev);
            }
        });

        return cardView;
    }

    /**
     * При нажатии на свободный час спрашиваем: выбрать из To-Do или создать новое событие?
     */
    private void showSlotChoiceDialog(final int presetStartMinutes) {
        String[] options = {"Pick task from To-Do List", "Create new custom event"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Schedule at " + ScheduleEvent.formatTime(presetStartMinutes))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showPickFromTodoDialog(presetStartMinutes);
                        } else {
                            showAddCustomEventDialog(presetStartMinutes);
                        }
                    }
                })
                .show();
    }

    /**
     * Диалог выбора задачи из списка To-Do для добавления в расписание выбранного дня.
     */
    private void showPickFromTodoDialog(@Nullable final Integer presetStartMinutes) {
        // Сначала берём задачи, которые ещё не запланированы полностью; если таких нет — берём все
        List<TodoItem> candidates = dbHelper.getRemainingTodoItems();
        if (candidates.isEmpty()) {
            candidates = dbHelper.getAllTodoItems();
        }

        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Your To-Do List is empty! Add tasks in the To-Do tab first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        final List<TodoItem> finalCandidates = candidates;
        String[] labels = new String[finalCandidates.size()];
        for (int i = 0; i < finalCandidates.size(); i++) {
            TodoItem t = finalCandidates.get(i);
            int done = dbHelper.getScheduledCountForTodo(t.getId());
            labels[i] = t.getTitle() + " (" + ScheduleEvent.formatDuration(t.getDurationMinutes())
                    + ", " + t.getCategory() + ") [" + done + "/" + t.getTargetCount() + "]";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Add from To-Do to " + ScheduleEvent.getDayFull(selectedDay))
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final TodoItem chosen = finalCandidates.get(which);
                        int startGuess = presetStartMinutes != null
                                ? presetStartMinutes
                                : dbHelper.findFirstFreeSlot(selectedDay,
                                chosen.getDurationMinutes(), chosen.getPrepMinutes());

                        new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                int startMin = hourOfDay * 60 + minute;
                                ScheduleEvent ev = new ScheduleEvent(
                                        chosen.getTitle(),
                                        selectedDay,
                                        startMin,
                                        chosen.getDurationMinutes(),
                                        chosen.getPrepMinutes(),
                                        chosen.getCategory(),
                                        chosen.getId(),
                                        false
                                );
                                dbHelper.addScheduleEvent(ev);
                                refreshDaySchedule();
                                Toast.makeText(requireContext(),
                                        "Added \"" + chosen.getTitle() + "\" at "
                                                + ScheduleEvent.formatTime(startMin),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, startGuess / 60, startGuess % 60, true).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Диалог ручного создания нового события сразу в расписание (например, уроки или секция).
     */
    private void showAddCustomEventDialog(int initialStartMinutes) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_event, null);

        final EditText etTitle = dialogView.findViewById(R.id.etEventTitle);
        final Button btnPickStart = dialogView.findViewById(R.id.btnPickEventStart);
        final EditText etDuration = dialogView.findViewById(R.id.etEventDuration);
        final EditText etPrep = dialogView.findViewById(R.id.etEventPrep);
        final Spinner spCategory = dialogView.findViewById(R.id.spEventCategory);
        final CheckBox cbWeekly = dialogView.findViewById(R.id.cbRepeatWeekly);

        spCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ScheduleEvent.CATEGORIES));

        final int[] chosenStart = {initialStartMinutes};
        btnPickStart.setText(ScheduleEvent.formatTime(chosenStart[0]));
        btnPickStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        chosenStart[0] = hourOfDay * 60 + minute;
                        btnPickStart.setText(ScheduleEvent.formatTime(chosenStart[0]));
                    }
                }, chosenStart[0] / 60, chosenStart[0] % 60, true).show();
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("New Event on " + ScheduleEvent.getDayFull(selectedDay))
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String title = etTitle.getText().toString().trim();
                        if (title.isEmpty()) {
                            Toast.makeText(requireContext(), "Enter event title", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int duration = 60;
                        try {
                            duration = Math.max(10, Integer.parseInt(etDuration.getText().toString().trim()));
                        } catch (Exception ignored) {
                        }
                        int prep = 0;
                        try {
                            prep = Math.max(0, Integer.parseInt(etPrep.getText().toString().trim()));
                        } catch (Exception ignored) {
                        }
                        String category = ScheduleEvent.CATEGORIES[spCategory.getSelectedItemPosition()];

                        ScheduleEvent ev = new ScheduleEvent(
                                title, selectedDay, chosenStart[0],
                                duration, prep, category, -1, cbWeekly.isChecked()
                        );
                        dbHelper.addScheduleEvent(ev);
                        refreshDaySchedule();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Диалог действий над существующим событием: перенести на другой день/время или удалить.
     */
    private void showEventActionsDialog(final ScheduleEvent ev) {
        String[] actions = {"Move to another day / time", "Remove from schedule"};
        new AlertDialog.Builder(requireContext())
                .setTitle(ev.getTitle())
                .setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            showMoveEventDialog(ev);
                        } else {
                            dbHelper.deleteScheduleEvent(ev.getId());
                            refreshDaySchedule();
                            Toast.makeText(requireContext(), "Event removed", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    /**
     * Перенос выбранного события на новый день недели и новое время.
     */
    private void showMoveEventDialog(final ScheduleEvent ev) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Move \"" + ev.getTitle() + "\" to day:")
                .setItems(ScheduleEvent.DAYS_FULL, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int newDay) {
                        new TimePickerDialog(requireContext(), new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                int newStart = hourOfDay * 60 + minute;
                                dbHelper.updateEventTime(ev.getId(), newDay, newStart);
                                selectedDay = newDay;
                                buildDaySelectorButtons();
                                refreshDaySchedule();
                                Toast.makeText(requireContext(),
                                        "Moved to " + ScheduleEvent.getDayShort(newDay)
                                                + " " + ScheduleEvent.formatTime(newStart),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }, ev.getStartMinutes() / 60, ev.getStartMinutes() % 60, true).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
