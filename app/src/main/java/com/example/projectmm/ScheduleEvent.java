package com.example.projectmm;

import java.util.Locale;

/**
 * Модель запланированного события в расписании на конкретный день и час.
 * Также содержит вспомогательные статические методы для форматирования времени и дней недели.
 */
public class ScheduleEvent {

    // Названия дней недели (неделя начинается с воскресенья: 0 = Sun ... 6 = Sat)
    public static final String[] DAYS_SHORT = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    public static final String[] DAYS_FULL = {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    // Доступные категории событий
    public static final String[] CATEGORIES = {
            "Study", "Sport", "Entertainment", "Hobby", "Chores"
    };

    // Доступные приоритеты и время суток
    public static final String[] PRIORITIES = {"Low", "Medium", "High"};
    public static final String[] PREFERRED_TIMES = {"Any", "Morning", "Afternoon", "Evening"};

    // Границы активного дня для расчёта загруженности: 07:00 (420 мин) — 23:00 (1380 мин) = 960 мин (16 ч)
    public static final int ACTIVE_DAY_START = 7 * 60;
    public static final int ACTIVE_DAY_END = 23 * 60;
    public static final int ACTIVE_DAY_MINUTES = ACTIVE_DAY_END - ACTIVE_DAY_START;

    private int id;                 // Уникальный ID события в SQLite
    private String title;           // Название события
    private int dayOfWeek;          // День недели: 0=Sun, 1=Mon, ..., 6=Sat
    private int startMinutes;       // Время начала события в минутах от полуночи (например, 540 = 09:00)
    private int durationMinutes;    // Длительность самого события в минутах
    private int prepMinutes;        // Время на подготовку/дорогу перед событием в минутах
    private String category;        // Категория события (влияет на цвет карточки и советы ИИ)
    private int todoId;             // ID связанной задачи из To-Do (-1, если добавлено напрямую)
    private boolean isWeekly;       // Повторяется ли каждую неделю

    public ScheduleEvent() {
    }

    public ScheduleEvent(int id, String title, int dayOfWeek, int startMinutes,
                         int durationMinutes, int prepMinutes, String category,
                         int todoId, boolean isWeekly) {
        this.id = id;
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.startMinutes = startMinutes;
        this.durationMinutes = durationMinutes;
        this.prepMinutes = prepMinutes;
        this.category = category;
        this.todoId = todoId;
        this.isWeekly = isWeekly;
    }

    public ScheduleEvent(String title, int dayOfWeek, int startMinutes,
                         int durationMinutes, int prepMinutes, String category,
                         int todoId, boolean isWeekly) {
        this(0, title, dayOfWeek, startMinutes, durationMinutes, prepMinutes, category, todoId, isWeekly);
    }

    /**
     * Фактическое начало занятости с учётом подготовки/дороги.
     * Например, если тренировка в 16:00, а дорога 30 мин, то человек занят с 15:30.
     */
    public int getEffectiveStartMinutes() {
        return Math.max(0, startMinutes - prepMinutes);
    }

    /**
     * Время окончания события в минутах от полуночи.
     */
    public int getEndMinutes() {
        return Math.min(24 * 60, startMinutes + durationMinutes);
    }

    /**
     * Суммарное время, которое занимает событие вместе с подготовкой.
     */
    public int getTotalBusyMinutes() {
        return durationMinutes + prepMinutes;
    }

    // --- Вспомогательные методы форматирования ---

    /**
     * Переводит минуты от полуночи в строку "HH:MM" (например, 540 -> "09:00").
     */
    public static String formatTime(int totalMinutes) {
        int clamped = Math.max(0, Math.min(24 * 60 - 1, totalMinutes));
        int hours = clamped / 60;
        int mins = clamped % 60;
        return String.format(Locale.US, "%02d:%02d", hours, mins);
    }

    /**
     * Переводит длительность в удобный текст (например, 90 -> "1h 30m", 45 -> "45m").
     */
    public static String formatDuration(int minutes) {
        if (minutes <= 0) {
            return "0m";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        if (h > 0 && m > 0) {
            return h + "h " + m + "m";
        } else if (h > 0) {
            return h + "h";
        } else {
            return m + "m";
        }
    }

    /**
     * Безопасно возвращает короткое имя дня недели по индексу (0..6).
     */
    public static String getDayShort(int day) {
        if (day >= 0 && day < DAYS_SHORT.length) {
            return DAYS_SHORT[day];
        }
        return "Sun";
    }

    /**
     * Безопасно возвращает полное имя дня недели по индексу (0..6).
     */
    public static String getDayFull(int day) {
        if (day >= 0 && day < DAYS_FULL.length) {
            return DAYS_FULL[day];
        }
        return "Sunday";
    }

    /**
     * Возвращает пастельный цвет фона карточки в зависимости от категории.
     */
    public static int getCategoryPastelColorRes(String category) {
        if ("Sport".equalsIgnoreCase(category)) {
            return R.color.orange_pastel;
        } else if ("Entertainment".equalsIgnoreCase(category)) {
            return R.color.pink_pastel;
        } else if ("Hobby".equalsIgnoreCase(category)) {
            return R.color.blue_pastel;
        } else if ("Chores".equalsIgnoreCase(category)) {
            return R.color.yellow_pastel;
        }
        return R.color.purple_pastel; // Study (по умолчанию)
    }

    /**
     * Возвращает яркий акцентный цвет текста/полоски в зависимости от категории.
     */
    public static int getCategoryNeonColorRes(String category) {
        if ("Sport".equalsIgnoreCase(category)) {
            return R.color.orange;
        } else if ("Entertainment".equalsIgnoreCase(category)) {
            return R.color.pink;
        } else if ("Hobby".equalsIgnoreCase(category)) {
            return R.color.blue;
        } else if ("Chores".equalsIgnoreCase(category)) {
            return R.color.orange_neon;
        }
        return R.color.purple; // Study (по умолчанию)
    }

    // --- Геттеры и сеттеры ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartMinutes() {
        return startMinutes;
    }

    public void setStartMinutes(int startMinutes) {
        this.startMinutes = startMinutes;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTodoId() {
        return todoId;
    }

    public void setTodoId(int todoId) {
        this.todoId = todoId;
    }

    public boolean isWeekly() {
        return isWeekly;
    }

    public void setWeekly(boolean weekly) {
        isWeekly = weekly;
    }
}
