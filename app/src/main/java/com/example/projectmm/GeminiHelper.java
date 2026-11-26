package com.example.projectmm;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с бесплатным Google Gemini API.
 * Использует только стандартные встроенные средства Java и Android:
 * - HttpURLConnection для отправки POST-запроса в интернет (в фоновом потоке Thread);
 * - org.json (JSONObject / JSONArray) для сборки запроса и разбора ответа.
 *
 * Никаких сторонних сложных библиотек (Retrofit, OkHttp, Gson) не требуется!
 */
public class GeminiHelper {

    // Базовый адрес бесплатного Gemini REST API
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String PRIMARY_MODEL = "gemini-2.5-flash";
    private static final String FALLBACK_MODEL = "gemini-2.0-flash";

    /**
     * Интерфейс обратного вызова (Callback), чтобы вернуть ответ из фонового потока на экран.
     */
    public interface GeminiCallback {
        void onSuccess(String replyText, List<AiAction> proposedActions);
        void onError(String errorMessage);
    }

    /**
     * Асинхронно отправляет сообщение пользователя вместе с текущим расписанием и To-Do списком в Gemini.
     */
    public static void sendMessageAsync(final String apiKey,
                                        final String userMessage,
                                        final List<ChatMessage> history,
                                        final String scheduleAndTodoContext,
                                        final GeminiCallback callback) {
        // Сетевые запросы в Android запрещено делать в главном потоке (UI thread),
        // поэтому запускаем обычный фоновый поток Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String rawJson = callGeminiModel(PRIMARY_MODEL, apiKey,
                            userMessage, history, scheduleAndTodoContext);
                    parseGeminiResponse(rawJson, callback);
                } catch (ModelNotFoundException e) {
                    // Если основная модель недоступна, пробуем запасную модель
                    try {
                        String rawJson = callGeminiModel(FALLBACK_MODEL, apiKey,
                                userMessage, history, scheduleAndTodoContext);
                        parseGeminiResponse(rawJson, callback);
                    } catch (Exception ex) {
                        callback.onError("Gemini API error: " + ex.getMessage());
                    }
                } catch (Exception e) {
                    callback.onError("Connection / API error: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Выполняет HTTP POST-запрос к серверу Google Gemini и возвращает строку ответа.
     */
    private static String callGeminiModel(String modelName,
                                          String apiKey,
                                          String userMessage,
                                          List<ChatMessage> history,
                                          String scheduleAndTodoContext) throws Exception {
        String endpoint = BASE_URL + modelName + ":generateContent?key=" + apiKey.trim();
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        // Формируем тело JSON-запроса по документации Gemini API
        JSONObject requestBody = new JSONObject();

        // 1. Системная инструкция: объясняем ИИ его роль, правила планирования и формат JSON-ответа
        JSONObject systemInstruction = new JSONObject();
        JSONArray sysParts = new JSONArray();
        sysParts.put(new JSONObject().put("text", buildSystemPrompt(scheduleAndTodoContext)));
        systemInstruction.put("parts", sysParts);
        requestBody.put("systemInstruction", systemInstruction);

        // 2. История последних сообщений чата + текущее сообщение пользователя
        JSONArray contents = new JSONArray();
        int startIdx = Math.max(0, history.size() - 8);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JSONObject turn = new JSONObject();
            turn.put("role", msg.isFromUser() ? "user" : "model");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", msg.getText()));
            turn.put("parts", parts);
            contents.put(turn);
        }

        // Текущий запрос пользователя
        JSONObject userTurn = new JSONObject();
        userTurn.put("role", "user");
        JSONArray userParts = new JSONArray();
        userParts.put(new JSONObject().put("text", userMessage));
        userTurn.put("parts", userParts);
        contents.put(userTurn);

        requestBody.put("contents", contents);

        // 3. Просим Gemini гарантированно вернуть структурированный JSON
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.4);
        requestBody.put("generationConfig", generationConfig);

        // Отправляем байты JSON-запроса на сервер
        byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream os = conn.getOutputStream();
        os.write(payload);
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        if (code == 404) {
            conn.disconnect();
            throw new ModelNotFoundException();
        }

        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code + ": " + sb.toString());
        }
        return sb.toString();
    }

    /**
     * Разбирает JSON-ответ от сервера Gemini: достаёт текст для пользователя и массив команд actions.
     */
    private static void parseGeminiResponse(String rawServerJson, GeminiCallback callback) throws Exception {
        JSONObject root = new JSONObject(rawServerJson);
        JSONArray candidates = root.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        String modelJsonText = content.getJSONArray("parts").getJSONObject(0).getString("text");

        JSONObject parsed = new JSONObject(modelJsonText);
        String reply = parsed.optString("reply", "Here is my suggestion:");

        List<AiAction> actions = new ArrayList<>();
        JSONArray actionsArr = parsed.optJSONArray("actions");
        if (actionsArr != null) {
            for (int i = 0; i < actionsArr.length(); i++) {
                JSONObject obj = actionsArr.getJSONObject(i);
                AiAction action = new AiAction();
                action.type = obj.optString("type", "").trim();
                action.title = obj.optString("title", "Task").trim();
                action.todoId = obj.optInt("todoId", -1);
                action.eventId = obj.optInt("eventId", -1);
                action.dayOfWeek = clampDay(obj.optInt("dayOfWeek", 0));

                // Читаем время в формате "HH:MM" (например "16:30") или в минутах
                String startTimeStr = obj.optString("startTime", "");
                if (!startTimeStr.isEmpty() && startTimeStr.contains(":")) {
                    action.startMinutes = parseTimeToMinutes(startTimeStr, 9 * 60);
                } else {
                    action.startMinutes = obj.optInt("startMinutes", 9 * 60);
                }

                action.durationMinutes = Math.max(15, obj.optInt("durationMinutes", 60));
                action.prepMinutes = Math.max(0, obj.optInt("prepMinutes", 0));
                action.category = normalizeCategory(obj.optString("category", "Study"));
                action.priority = obj.optString("priority", "Medium");
                action.preferredTime = obj.optString("preferredTime", "Any");
                action.deadlineDay = obj.optInt("deadlineDay", -1);
                action.repeatType = obj.optString("repeatType", TodoItem.REPEAT_ONCE);
                action.targetCount = Math.max(1, obj.optInt("targetCount", 1));

                if (!action.type.isEmpty()) {
                    actions.add(action);
                }
            }
        }

        callback.onSuccess(reply, actions);
    }

    /**
     * Собирает полную сводку расписания на неделю, загруженности по дням и задач To-Do из SQLite,
     * чтобы ИИ видел актуальную картину пользователя перед ответом.
     */
    public static String buildDatabaseContext(DatabaseHelper dbHelper) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== DAILY WORKLOAD (Week starts on Sunday: 0=Sun..6=Sat) ===\n");
        for (int d = 0; d < 7; d++) {
            int pct = dbHelper.getDayWorkloadPercent(d);
            int busyMin = dbHelper.getDayBusyMinutes(d);
            sb.append("Day ").append(d).append(" (").append(ScheduleEvent.DAYS_FULL[d]).append("): ")
                    .append(pct).append("% busy (").append(ScheduleEvent.formatDuration(busyMin)).append(")\n");
        }

        sb.append("\n=== CURRENT SCHEDULED EVENTS ===\n");
        List<ScheduleEvent> events = dbHelper.getAllScheduleEvents();
        if (events.isEmpty()) {
            sb.append("(No events scheduled yet)\n");
        } else {
            for (ScheduleEvent ev : events) {
                sb.append("- [eventId=").append(ev.getId()).append("] ")
                        .append(ScheduleEvent.DAYS_FULL[ev.getDayOfWeek()])
                        .append(" (day=").append(ev.getDayOfWeek()).append(") ")
                        .append(ScheduleEvent.formatTime(ev.getStartMinutes()))
                        .append("-").append(ScheduleEvent.formatTime(ev.getEndMinutes()))
                        .append(" | title=\"").append(ev.getTitle()).append("\"")
                        .append(" | category=").append(ev.getCategory())
                        .append(" | prep=").append(ev.getPrepMinutes()).append("m")
                        .append(" (busy from ").append(ScheduleEvent.formatTime(ev.getEffectiveStartMinutes())).append(")")
                        .append(" | todoId=").append(ev.getTodoId())
                        .append("\n");
            }
        }

        sb.append("\n=== TO-DO LIST TASKS ===\n");
        List<TodoItem> todos = dbHelper.getAllTodoItems();
        if (todos.isEmpty()) {
            sb.append("(To-Do List is empty)\n");
        } else {
            for (TodoItem t : todos) {
                int scheduledCount = dbHelper.getScheduledCountForTodo(t.getId());
                sb.append("- [todoId=").append(t.getId()).append("] ")
                        .append("title=\"").append(t.getTitle()).append("\"")
                        .append(" | duration=").append(t.getDurationMinutes()).append("m")
                        .append(" | prep=").append(t.getPrepMinutes()).append("m")
                        .append(" | category=").append(t.getCategory())
                        .append(" | priority=").append(t.getPriority())
                        .append(" | preferredTime=").append(t.getPreferredTime())
                        .append(" | deadlineDay=").append(t.getDeadlineDay() >= 0
                                ? ScheduleEvent.DAYS_FULL[t.getDeadlineDay()] + "(" + t.getDeadlineDay() + ")"
                                : "None")
                        .append(" | repeat=").append(t.getRepeatType())
                        .append(" | scheduled=").append(scheduledCount).append("/").append(t.getTargetCount())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Формирует системный промпт для Gemini с умными правилами планирования и схемой JSON.
     */
    private static String buildSystemPrompt(String dbContext) {
        return "You are Smart AI Planner inside a student's Smart Schedule Android app.\n"
                + "Reply in the SAME language the user writes in (English, Russian, etc.). Keep explanations friendly, clear, and concise.\n\n"
                + "WEEK INDEXING (Starts on Sunday!):\n"
                + "0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday.\n\n"
                + "SMART SCHEDULING RULES YOU MUST FOLLOW:\n"
                + "1. Neighboring events & recovery: Avoid placing intensive 'Study' immediately after 'Sport' (physical fatigue makes studying hard — leave at least a 45-60m rest gap or place 'Entertainment'/'Hobby' in between). Avoid >2.5 hours of non-stop 'Study' without a break.\n"
                + "2. Daily workload balance: Check daily workload %. Prefer lighter days (<65%) and avoid overloading heavy days (>75%).\n"
                + "3. Prep/travel time: Every event blocks time from (startTime - prepMinutes) to (startTime + durationMinutes). Never create overlapping busy intervals!\n"
                + "4. Active hours: Schedule tasks within reasonable awake hours (07:30 to 22:30), respecting preferredTime (Morning 07:30-12:00, Afternoon 12:00-17:00, Evening 17:00-22:00), priority (High first), deadlineDay (on or before deadline), and targetCount (if 2x/week and 0/2 scheduled, propose 2 slots on different days).\n\n"
                + "SUPPORTED ACTIONS (include in 'actions' array ONLY when user asks to add, schedule, move, remove, or optimize/propose schedule changes; leave 'actions' empty [] if just chatting/discussing):\n"
                + "- ADD_TODO: Add a new task to To-Do List. Fields: type='ADD_TODO', title, durationMinutes, prepMinutes, category ('Study'|'Sport'|'Entertainment'|'Hobby'|'Chores'), priority ('Low'|'Medium'|'High'), preferredTime ('Any'|'Morning'|'Afternoon'|'Evening'), deadlineDay (-1 or 0..6), repeatType ('ONCE'|'TIMES_PER_WEEK'), targetCount.\n"
                + "- SCHEDULE_TODO: Place an existing To-Do item into the weekly schedule. Fields: type='SCHEDULE_TODO', todoId, title, dayOfWeek (0..6), startTime ('HH:MM' 24h), durationMinutes, prepMinutes, category.\n"
                + "- ADD_EVENT: Add a brand new event directly onto the schedule. Fields: type='ADD_EVENT', title, dayOfWeek (0..6), startTime ('HH:MM' 24h), durationMinutes, prepMinutes, category.\n"
                + "- MOVE_EVENT: Move an existing scheduled event to a new day/time. Fields: type='MOVE_EVENT', eventId, title, dayOfWeek (0..6), startTime ('HH:MM' 24h).\n"
                + "- DELETE_EVENT: Remove an existing scheduled event. Fields: type='DELETE_EVENT', eventId, title.\n\n"
                + "OUTPUT FORMAT (strictly JSON object):\n"
                + "{\n"
                + "  \"reply\": \"Your helpful message explaining reasoning (mentioning workload & neighboring events when scheduling)\",\n"
                + "  \"actions\": [ ... ]\n"
                + "}\n\n"
                + "USER'S CURRENT DATA:\n"
                + dbContext;
    }

    private static int parseTimeToMinutes(String hhmm, int fallback) {
        try {
            String[] parts = hhmm.trim().split(":");
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h >= 0 && h < 24 && m >= 0 && m < 60) {
                return h * 60 + m;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static int clampDay(int d) {
        return Math.max(0, Math.min(6, d));
    }

    private static String normalizeCategory(String cat) {
        for (String valid : ScheduleEvent.CATEGORIES) {
            if (valid.equalsIgnoreCase(cat.trim())) {
                return valid;
            }
        }
        return "Study";
    }

    private static class ModelNotFoundException extends Exception {
    }
}
