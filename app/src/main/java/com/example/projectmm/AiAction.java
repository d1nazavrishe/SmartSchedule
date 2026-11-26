package com.example.projectmm;

/**
 * Команда (действие), которую предлагает выполнить ИИ-помощник.
 * Например: добавить задачу в To-Do, поставить задачу из To-Do в расписание,
 * переместить существующее событие или удалить его.
 */
public class AiAction {

    // Типы поддерживаемых команд от ИИ
    public static final String TYPE_ADD_TODO = "ADD_TODO";             // Добавить новую задачу в To-Do List
    public static final String TYPE_SCHEDULE_TODO = "SCHEDULE_TODO";   // Поставить задачу из To-Do в расписание
    public static final String TYPE_ADD_EVENT = "ADD_EVENT";           // Создать событие сразу в расписании
    public static final String TYPE_MOVE_EVENT = "MOVE_EVENT";         // Перенести событие на другое время/день
    public static final String TYPE_DELETE_EVENT = "DELETE_EVENT";     // Удалить событие из расписания

    public String type;             // Один из константных типов выше
    public String title;            // Название задачи или события
    public int todoId = -1;         // ID задачи в To-Do (для SCHEDULE_TODO)
    public int eventId = -1;        // ID события в расписании (для MOVE_EVENT / DELETE_EVENT)
    public int dayOfWeek = 0;       // День недели (0=Sun..6=Sat)
    public int startMinutes = 540;  // Время начала в минутах от полуночи
    public int durationMinutes = 60;// Длительность в минутах
    public int prepMinutes = 0;     // Время подготовки в минутах
    public String category = "Study";
    public String priority = "Medium";
    public String preferredTime = "Any";
    public int deadlineDay = -1;
    public String repeatType = TodoItem.REPEAT_ONCE;
    public int targetCount = 1;

    /**
     * Формирует понятное текстовое описание действия для карточки подтверждения в чате.
     */
    public String toReadableSummary() {
        if (TYPE_ADD_TODO.equals(type)) {
            return "+ Add to To-Do: \"" + title + "\" ("
                    + ScheduleEvent.formatDuration(durationMinutes) + ", " + category + ")";
        } else if (TYPE_SCHEDULE_TODO.equals(type)) {
            return "Schedule To-Do \"" + title + "\" -> "
                    + ScheduleEvent.getDayShort(dayOfWeek) + " at "
                    + ScheduleEvent.formatTime(startMinutes);
        } else if (TYPE_ADD_EVENT.equals(type)) {
            return "+ Add to Schedule: \"" + title + "\" -> "
                    + ScheduleEvent.getDayShort(dayOfWeek) + " at "
                    + ScheduleEvent.formatTime(startMinutes) + " ("
                    + ScheduleEvent.formatDuration(durationMinutes) + ")";
        } else if (TYPE_MOVE_EVENT.equals(type)) {
            return "Move \"" + title + "\" -> "
                    + ScheduleEvent.getDayShort(dayOfWeek) + " at "
                    + ScheduleEvent.formatTime(startMinutes);
        } else if (TYPE_DELETE_EVENT.equals(type)) {
            return "Remove \"" + title + "\" from Schedule";
        }
        return title != null ? title : "Update schedule";
    }
}
