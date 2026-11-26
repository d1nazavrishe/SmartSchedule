package com.example.projectmm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Помощник для работы с локальной базой данных SQLite.
 * Содержит две таблицы:
 * 1) todo_items — список задач (To-Do List) со всеми параметрами.
 * 2) schedule_events — события, расставленные по дням недели и часам.
 *
 * Также считает загруженность дня и ищет свободные окна во времени.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "smart_schedule.db";
    private static final int DATABASE_VERSION = 1;

    // --- Таблица To-Do задач ---
    public static final String TABLE_TODO = "todo_items";
    private static final String COL_TODO_ID = "id";
    private static final String COL_TODO_TITLE = "title";
    private static final String COL_TODO_DURATION = "duration_minutes";
    private static final String COL_TODO_PREP = "prep_minutes";
    private static final String COL_TODO_CATEGORY = "category";
    private static final String COL_TODO_PRIORITY = "priority";
    private static final String COL_TODO_PREF_TIME = "preferred_time";
    private static final String COL_TODO_DEADLINE = "deadline_day";
    private static final String COL_TODO_REPEAT_TYPE = "repeat_type";
    private static final String COL_TODO_TARGET_COUNT = "target_count";
    private static final String COL_TODO_FIXED_DAY = "fixed_day";
    private static final String COL_TODO_FIXED_START = "fixed_start_minutes";

    // --- Таблица событий расписания ---
    public static final String TABLE_EVENTS = "schedule_events";
    private static final String COL_EVENT_ID = "id";
    private static final String COL_EVENT_TITLE = "title";
    private static final String COL_EVENT_DAY = "day_of_week";
    private static final String COL_EVENT_START = "start_minutes";
    private static final String COL_EVENT_DURATION = "duration_minutes";
    private static final String COL_EVENT_PREP = "prep_minutes";
    private static final String COL_EVENT_CATEGORY = "category";
    private static final String COL_EVENT_TODO_ID = "todo_id";
    private static final String COL_EVENT_IS_WEEKLY = "is_weekly";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Вызывается автоматически при первом создании базы данных на телефоне.
     * Создаёт обе таблицы с помощью SQL-запроса CREATE TABLE.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTodoTable = "CREATE TABLE " + TABLE_TODO + " ("
                + COL_TODO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TODO_TITLE + " TEXT NOT NULL, "
                + COL_TODO_DURATION + " INTEGER NOT NULL, "
                + COL_TODO_PREP + " INTEGER DEFAULT 0, "
                + COL_TODO_CATEGORY + " TEXT NOT NULL, "
                + COL_TODO_PRIORITY + " TEXT DEFAULT 'Medium', "
                + COL_TODO_PREF_TIME + " TEXT DEFAULT 'Any', "
                + COL_TODO_DEADLINE + " INTEGER DEFAULT -1, "
                + COL_TODO_REPEAT_TYPE + " TEXT DEFAULT 'ONCE', "
                + COL_TODO_TARGET_COUNT + " INTEGER DEFAULT 1, "
                + COL_TODO_FIXED_DAY + " INTEGER DEFAULT -1, "
                + COL_TODO_FIXED_START + " INTEGER DEFAULT -1)";
        db.execSQL(createTodoTable);

        String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " ("
                + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EVENT_TITLE + " TEXT NOT NULL, "
                + COL_EVENT_DAY + " INTEGER NOT NULL, "
                + COL_EVENT_START + " INTEGER NOT NULL, "
                + COL_EVENT_DURATION + " INTEGER NOT NULL, "
                + COL_EVENT_PREP + " INTEGER DEFAULT 0, "
                + COL_EVENT_CATEGORY + " TEXT NOT NULL, "
                + COL_EVENT_TODO_ID + " INTEGER DEFAULT -1, "
                + COL_EVENT_IS_WEEKLY + " INTEGER DEFAULT 0)";
        db.execSQL(createEventsTable);
    }

    /**
     * Вызывается при увеличении DATABASE_VERSION. Пересоздаёт таблицы.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODO);
        onCreate(db);
    }

    // ==================== МЕТОДЫ ДЛЯ TO-DO LIST ====================

    /**
     * Добавляет новую задачу в таблицу todo_items.
     * Если задача имеет режим WEEKLY_FIXED (каждую неделю в определённое время),
     * она сразу автоматически добавляется и в расписание на выбранный день и час.
     */
    public long addTodoItem(TodoItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TODO_TITLE, item.getTitle());
        values.put(COL_TODO_DURATION, item.getDurationMinutes());
        values.put(COL_TODO_PREP, item.getPrepMinutes());
        values.put(COL_TODO_CATEGORY, item.getCategory());
        values.put(COL_TODO_PRIORITY, item.getPriority());
        values.put(COL_TODO_PREF_TIME, item.getPreferredTime());
        values.put(COL_TODO_DEADLINE, item.getDeadlineDay());
        values.put(COL_TODO_REPEAT_TYPE, item.getRepeatType());
        values.put(COL_TODO_TARGET_COUNT, item.getTargetCount());
        values.put(COL_TODO_FIXED_DAY, item.getFixedDay());
        values.put(COL_TODO_FIXED_START, item.getFixedStartMinutes());

        long newTodoId = db.insert(TABLE_TODO, null, values);

        // Если выбрано "каждую неделю в конкретное время", сразу ставим событие в расписание
        if (newTodoId != -1
                && TodoItem.REPEAT_WEEKLY_FIXED.equals(item.getRepeatType())
                && item.getFixedDay() >= 0
                && item.getFixedStartMinutes() >= 0) {
            ScheduleEvent fixedEvent = new ScheduleEvent(
                    item.getTitle(),
                    item.getFixedDay(),
                    item.getFixedStartMinutes(),
                    item.getDurationMinutes(),
                    item.getPrepMinutes(),
                    item.getCategory(),
                    (int) newTodoId,
                    true
            );
            addScheduleEvent(fixedEvent);
        }

        return newTodoId;
    }

    /**
     * Возвращает все задачи из списка To-Do (новые сверху).
     */
    public List<TodoItem> getAllTodoItems() {
        List<TodoItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TODO, null, null, null,
                null, null, COL_TODO_ID + " DESC");

        while (cursor.moveToNext()) {
            list.add(cursorToTodo(cursor));
        }
        cursor.close();
        return list;
    }

    /**
     * Находит одну задачу To-Do по её ID (или возвращает null, если не найдена).
     */
    public TodoItem getTodoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TODO, null, COL_TODO_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        TodoItem item = null;
        if (cursor.moveToFirst()) {
            item = cursorToTodo(cursor);
        }
        cursor.close();
        return item;
    }

    /**
     * Находит задачу To-Do по названию без учёта регистра (удобно для команд ИИ).
     */
    public TodoItem findTodoByTitle(String title) {
        if (title == null) {
            return null;
        }
        List<TodoItem> all = getAllTodoItems();
        for (TodoItem item : all) {
            if (item.getTitle().equalsIgnoreCase(title.trim())) {
                return item;
            }
        }
        // Если точного совпадения нет, ищем частичное вхождение
        for (TodoItem item : all) {
            if (item.getTitle().toLowerCase().contains(title.trim().toLowerCase())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Удаляет задачу из To-Do по её ID.
     */
    public void deleteTodoItem(int todoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TODO, COL_TODO_ID + " = ?", new String[]{String.valueOf(todoId)});
    }

    /**
     * Считает, сколько раз данная задача из To-Do уже поставлена в расписание на неделю.
     * Это нужно для задач с параметром "сделать 2-3 раза в неделю".
     */
    public int getScheduledCountForTodo(int todoId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_EVENTS + " WHERE " + COL_EVENT_TODO_ID + " = ?",
                new String[]{String.valueOf(todoId)}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Возвращает список задач из To-Do, которые ещё нужно поставить в расписание
     * (то есть количество запланированных раз меньше targetCount).
     */
    public List<TodoItem> getRemainingTodoItems() {
        List<TodoItem> all = getAllTodoItems();
        List<TodoItem> remaining = new ArrayList<>();
        for (TodoItem item : all) {
            int scheduled = getScheduledCountForTodo(item.getId());
            if (scheduled < item.getTargetCount()) {
                remaining.add(item);
            }
        }
        return remaining;
    }

    private TodoItem cursorToTodo(Cursor cursor) {
        return new TodoItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TODO_TITLE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_DURATION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_PREP)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TODO_CATEGORY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TODO_PRIORITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TODO_PREF_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_DEADLINE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TODO_REPEAT_TYPE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_TARGET_COUNT)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_FIXED_DAY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TODO_FIXED_START))
        );
    }

    // ==================== МЕТОДЫ ДЛЯ РАСПИСАНИЯ ====================

    /**
     * Добавляет событие в расписание.
     */
    public long addScheduleEvent(ScheduleEvent event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_TITLE, event.getTitle());
        values.put(COL_EVENT_DAY, event.getDayOfWeek());
        values.put(COL_EVENT_START, event.getStartMinutes());
        values.put(COL_EVENT_DURATION, event.getDurationMinutes());
        values.put(COL_EVENT_PREP, event.getPrepMinutes());
        values.put(COL_EVENT_CATEGORY, event.getCategory());
        values.put(COL_EVENT_TODO_ID, event.getTodoId());
        values.put(COL_EVENT_IS_WEEKLY, event.isWeekly() ? 1 : 0);
        return db.insert(TABLE_EVENTS, null, values);
    }

    /**
     * Возвращает все события на выбранный день недели (0=Sun..6=Sat),
     * отсортированные по времени начала (от утра к вечеру).
     */
    public List<ScheduleEvent> getEventsForDay(int dayOfWeek) {
        List<ScheduleEvent> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EVENTS, null,
                COL_EVENT_DAY + " = ?",
                new String[]{String.valueOf(dayOfWeek)},
                null, null,
                COL_EVENT_START + " ASC");

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor));
        }
        cursor.close();
        return events;
    }

    /**
     * Возвращает вообще все запланированные события на всю неделю.
     */
    public List<ScheduleEvent> getAllScheduleEvents() {
        List<ScheduleEvent> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_EVENTS, null, null, null,
                null, null, COL_EVENT_DAY + " ASC, " + COL_EVENT_START + " ASC");

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor));
        }
        cursor.close();
        return events;
    }

    /**
     * Переносит существующее событие на новый день недели и новое время начала.
     */
    public void updateEventTime(int eventId, int newDayOfWeek, int newStartMinutes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_DAY, newDayOfWeek);
        values.put(COL_EVENT_START, newStartMinutes);
        db.update(TABLE_EVENTS, values, COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)});
    }

    /**
     * Удаляет событие из расписания по его ID.
     */
    public void deleteScheduleEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EVENTS, COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)});
    }

    /**
     * Находит событие по названию (удобно, если ИИ передал название вместо ID).
     */
    public ScheduleEvent findEventByTitle(String title) {
        if (title == null) {
            return null;
        }
        List<ScheduleEvent> all = getAllScheduleEvents();
        for (ScheduleEvent ev : all) {
            if (ev.getTitle().equalsIgnoreCase(title.trim())) {
                return ev;
            }
        }
        for (ScheduleEvent ev : all) {
            if (ev.getTitle().toLowerCase().contains(title.trim().toLowerCase())) {
                return ev;
            }
        }
        return null;
    }

    private ScheduleEvent cursorToEvent(Cursor cursor) {
        return new ScheduleEvent(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_DAY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_START)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_DURATION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_PREP)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_CATEGORY)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_TODO_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_EVENT_IS_WEEKLY)) == 1
        );
    }

    // ==================== ЗАГРУЖЕННОСТЬ ДНЯ И СВОБОДНЫЕ ОКНА ====================

    /**
     * Считает общее занятое время за день (в минутах), учитывая время на подготовку.
     * Использует массив минут суток (1440 минут), чтобы при пересечении двух событий
     * общие минуты не считались дважды.
     */
    public int getDayBusyMinutes(int dayOfWeek) {
        List<ScheduleEvent> events = getEventsForDay(dayOfWeek);
        boolean[] busyMinute = new boolean[24 * 60];

        for (ScheduleEvent ev : events) {
            int start = Math.max(0, ev.getEffectiveStartMinutes());
            int end = Math.min(24 * 60, ev.getEndMinutes());
            for (int m = start; m < end; m++) {
                busyMinute[m] = true;
            }
        }

        int total = 0;
        for (boolean b : busyMinute) {
            if (b) {
                total++;
            }
        }
        return total;
    }

    /**
     * Считает процент загруженности активного дня (с 07:00 до 23:00 = 960 минут).
     * Возвращает число от 0 до 100.
     */
    public int getDayWorkloadPercent(int dayOfWeek) {
        int busy = getDayBusyMinutes(dayOfWeek);
        int percent = Math.round((busy * 100f) / ScheduleEvent.ACTIVE_DAY_MINUTES);
        return Math.max(0, Math.min(100, percent));
    }

    /**
     * Ищет первое свободное окно в выбранный день (между 08:00 и 22:00),
     * куда помещается задача заданной длительности + время на подготовку.
     * Возвращает рекомендуемое время начала (startMinutes), например 540 для 09:00.
     */
    public int findFirstFreeSlot(int dayOfWeek, int durationMinutes, int prepMinutes) {
        List<ScheduleEvent> events = getEventsForDay(dayOfWeek);
        boolean[] busy = new boolean[24 * 60];
        for (ScheduleEvent ev : events) {
            int s = Math.max(0, ev.getEffectiveStartMinutes());
            int e = Math.min(24 * 60, ev.getEndMinutes());
            for (int m = s; m < e; m++) {
                busy[m] = true;
            }
        }

        // Проверяем время с 08:00 (480) до 22:00 (1320) с шагом 15 минут
        for (int candidateStart = 8 * 60; candidateStart + durationMinutes <= 22 * 60; candidateStart += 15) {
            int effectiveStart = candidateStart - prepMinutes;
            if (effectiveStart < 7 * 60) {
                continue;
            }
            boolean fits = true;
            for (int m = effectiveStart; m < candidateStart + durationMinutes; m++) {
                if (busy[m]) {
                    fits = false;
                    break;
                }
            }
            if (fits) {
                return candidateStart;
            }
        }
        // Если свободного окна не нашлось, возвращаем 09:00 по умолчанию
        return 9 * 60;
    }
}
