package com.example.projectmm;

/**
 * Модель задачи из списка дел (To-Do List).
 * Хранит все параметры задачи: название, длительность, время на подготовку,
 * категорию, приоритет, желаемое время суток, дедлайн и режим повторения.
 */
public class TodoItem {

    // Константы режимов повторения задачи
    public static final String REPEAT_ONCE = "ONCE";                     // Сделать 1 раз
    public static final String REPEAT_TIMES_PER_WEEK = "TIMES_PER_WEEK"; // Сделать N раз в неделю
    public static final String REPEAT_WEEKLY_FIXED = "WEEKLY_FIXED";     // Каждую неделю в точное время

    private int id;                 // Уникальный ID задачи в таблице SQLite
    private String title;           // Название задачи (например, "Math Homework")
    private int durationMinutes;    // Длительность выполнения в минутах (например, 60)
    private int prepMinutes;        // Время на подготовку или дорогу в минутах (например, 15)
    private String category;        // Категория: Study, Sport, Entertainment, Hobby, Chores
    private String priority;        // Приоритет: Low, Medium, High
    private String preferredTime;   // Желаемое время суток: Any, Morning, Afternoon, Evening
    private int deadlineDay;        // День недели дедлайна (0=Sun..6=Sat) или -1 если без дедлайна
    private String repeatType;      // Режим повторения (ONCE, TIMES_PER_WEEK, WEEKLY_FIXED)
    private int targetCount;        // Сколько раз в неделю нужно выполнить (1, 2, 3...)
    private int fixedDay;           // Фиксированный день (0..6) для WEEKLY_FIXED, иначе -1
    private int fixedStartMinutes;  // Фиксированное время начала в минутах от 00:00, иначе -1

    // Пустой конструктор
    public TodoItem() {
    }

    // Полный конструктор (используется при чтении из БД и создании задачи)
    public TodoItem(int id, String title, int durationMinutes, int prepMinutes,
                    String category, String priority, String preferredTime,
                    int deadlineDay, String repeatType, int targetCount,
                    int fixedDay, int fixedStartMinutes) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.prepMinutes = prepMinutes;
        this.category = category;
        this.priority = priority;
        this.preferredTime = preferredTime;
        this.deadlineDay = deadlineDay;
        this.repeatType = repeatType;
        this.targetCount = targetCount;
        this.fixedDay = fixedDay;
        this.fixedStartMinutes = fixedStartMinutes;
    }

    // Удобный конструктор для новой задачи (без id, так как его назначит SQLite)
    public TodoItem(String title, int durationMinutes, int prepMinutes,
                    String category, String priority, String preferredTime,
                    int deadlineDay, String repeatType, int targetCount,
                    int fixedDay, int fixedStartMinutes) {
        this(0, title, durationMinutes, prepMinutes, category, priority,
                preferredTime, deadlineDay, repeatType, targetCount, fixedDay, fixedStartMinutes);
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(String preferredTime) {
        this.preferredTime = preferredTime;
    }

    public int getDeadlineDay() {
        return deadlineDay;
    }

    public void setDeadlineDay(int deadlineDay) {
        this.deadlineDay = deadlineDay;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public int getFixedDay() {
        return fixedDay;
    }

    public void setFixedDay(int fixedDay) {
        this.fixedDay = fixedDay;
    }

    public int getFixedStartMinutes() {
        return fixedStartMinutes;
    }

    public void setFixedStartMinutes(int fixedStartMinutes) {
        this.fixedStartMinutes = fixedStartMinutes;
    }
}
