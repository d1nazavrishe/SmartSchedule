package com.example.projectmm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран 3: Общение с умным ИИ-помощником (Smart AI Planner на базе Gemini API).
 * Возможности:
 * - Обсуждение любых задач из To-Do List и событий из расписания.
 * - Добавление задач из слов пользователя в чате прямо в To-Do List (ADD_TODO).
 * - Постановка задач из To-Do в расписание с учётом соседних событий (например, отдых после спорта)
 *   и общей загруженности каждого дня (SCHEDULE_TODO).
 * - Перемещение событий по расписанию по пожеланиям пользователя (MOVE_EVENT).
 * - Предложение оптимального расписания на неделю.
 * - Ключ Gemini API безопасно хранится локально на телефоне в SharedPreferences.
 */
public class AiChatFragment extends Fragment {

    private static final String AI_PREFS = "ai_prefs";
    private static final String KEY_GEMINI_API = "gemini_api_key";

    // Список сообщений сохраняется в памяти на время работы приложения
    private static final List<ChatMessage> chatHistory = new ArrayList<>();

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private ScrollView scrollChat;
    private LinearLayout llChatMessages;
    private ProgressBar pbAiLoading;
    private EditText etChatInput;
    private ImageButton btnSendChat;
    private MaterialButton btnApiKey;
    private MaterialButton btnPromptPlaceTodos;
    private MaterialButton btnPromptOptimize;
    private MaterialButton btnPromptAnalyze;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_chat, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        prefs = requireContext().getSharedPreferences(AI_PREFS, Context.MODE_PRIVATE);

        scrollChat = view.findViewById(R.id.scrollChat);
        llChatMessages = view.findViewById(R.id.llChatMessages);
        pbAiLoading = view.findViewById(R.id.pbAiLoading);
        etChatInput = view.findViewById(R.id.etChatInput);
        btnSendChat = view.findViewById(R.id.btnSendChat);
        btnApiKey = view.findViewById(R.id.btnApiKey);
        btnPromptPlaceTodos = view.findViewById(R.id.btnPromptPlaceTodos);
        btnPromptOptimize = view.findViewById(R.id.btnPromptOptimize);
        btnPromptAnalyze = view.findViewById(R.id.btnPromptAnalyze);

        // Приветственное сообщение ИИ при первом открытии экрана
        if (chatHistory.isEmpty()) {
            chatHistory.add(new ChatMessage(
                    "Hi! I'm your Smart AI Planner.\n\n"
                            + "I can see your Weekly Schedule, daily workload %, and your To-Do List. You can ask me to:\n"
                            + "• Add new tasks to your To-Do List from chat\n"
                            + "• Find the best time for To-Do tasks (considering sport/study balance & daily load)\n"
                            + "• Move events around or build an optimal schedule!",
                    false
            ));
        }

        // Кнопка ввода/обновления бесплатного Gemini API ключа
        btnApiKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiKeyDialog();
            }
        });

        // Отправка сообщения
        btnSendChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etChatInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    etChatInput.setText("");
                    sendUserMessage(text);
                }
            }
        });

        // Быстрые кнопки-запросы для удобного тестирования и демонстрации на защите
        btnPromptPlaceTodos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserMessage("Please suggest when and where to add my unscheduled To-Do tasks, considering neighboring events (like avoiding study right after sport) and daily workload.");
            }
        });

        btnPromptOptimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserMessage("Suggest an optimal weekly schedule for my current events and To-Do list so no day is overloaded.");
            }
        });

        btnPromptAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserMessage("Analyze my current schedule and daily workload. Are there any tough transitions like studying right after sport?");
            }
        });

        renderAllMessages();
        return view;
    }

    /**
     * Отправляет текст пользователя в Gemini API, прикрепив свежие данные из SQLite.
     */
    private void sendUserMessage(String messageText) {
        String apiKey = prefs.getString(KEY_GEMINI_API, "").trim();
        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please paste your free Gemini API key first!", Toast.LENGTH_LONG).show();
            showApiKeyDialog();
            etChatInput.setText(messageText);
            return;
        }

        // Собираем историю ДО добавления текущего сообщения, чтобы не продублировать его в запросе
        List<ChatMessage> previousHistory = new ArrayList<>(chatHistory);

        ChatMessage userMsg = new ChatMessage(messageText, true);
        chatHistory.add(userMsg);
        renderAllMessages();

        pbAiLoading.setVisibility(View.VISIBLE);
        btnSendChat.setEnabled(false);

        String dbContext = GeminiHelper.buildDatabaseContext(dbHelper);

        GeminiHelper.sendMessageAsync(apiKey, messageText, previousHistory, dbContext,
                new GeminiHelper.GeminiCallback() {
                    @Override
                    public void onSuccess(final String replyText, final List<AiAction> proposedActions) {
                        if (!isAdded()) {
                            return;
                        }
                        // Возвращаемся в главный поток (UI thread) для обновления экрана
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbAiLoading.setVisibility(View.GONE);
                                btnSendChat.setEnabled(true);
                                chatHistory.add(new ChatMessage(replyText, false, proposedActions));
                                renderAllMessages();
                            }
                        });
                    }

                    @Override
                    public void onError(final String errorMessage) {
                        if (!isAdded()) {
                            return;
                        }
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbAiLoading.setVisibility(View.GONE);
                                btnSendChat.setEnabled(true);
                                chatHistory.add(new ChatMessage(
                                        "Could not reach Gemini API.\nDetails: " + errorMessage
                                                + "\n\nTip: Check your API key via the 'API Key' button above.",
                                        false));
                                renderAllMessages();
                            }
                        });
                    }
                });
    }

    /**
     * Отрисовывает все сообщения чата и карточки подтверждения действий (Apply / Dismiss).
     */
    private void renderAllMessages() {
        llChatMessages.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (final ChatMessage msg : chatHistory) {
            View row = inflater.inflate(R.layout.item_chat_message, llChatMessages, false);

            LinearLayout wrapper = row.findViewById(R.id.llMessageWrapper);
            MaterialCardView cardBubble = row.findViewById(R.id.cardBubble);
            TextView tvText = row.findViewById(R.id.tvMessageText);
            MaterialCardView cardActions = row.findViewById(R.id.cardProposedActions);
            TextView tvActionsSummary = row.findViewById(R.id.tvActionsSummary);
            LinearLayout llActionButtons = row.findViewById(R.id.llActionButtons);
            MaterialButton btnApply = row.findViewById(R.id.btnApplyActions);
            MaterialButton btnDismiss = row.findViewById(R.id.btnDismissActions);
            TextView tvResolvedStatus = row.findViewById(R.id.tvActionResolvedStatus);

            tvText.setText(msg.getText());

            if (msg.isFromUser()) {
                // Сообщение пользователя — справа, на фиолетовом фоне с белым текстом
                wrapper.setGravity(Gravity.END);
                cardBubble.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.purple_neon));
                cardBubble.setStrokeWidth(0);
                tvText.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface));
                cardActions.setVisibility(View.GONE);
            } else {
                // Сообщение ИИ — слева, на белом фоне
                wrapper.setGravity(Gravity.START);
                cardBubble.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.surface));
                tvText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

                // Если ИИ предложил действия над расписанием или To-Do — показываем карточку
                if (msg.getActions() != null && !msg.getActions().isEmpty()) {
                    cardActions.setVisibility(View.VISIBLE);
                    StringBuilder summary = new StringBuilder();
                    for (int i = 0; i < msg.getActions().size(); i++) {
                        if (i > 0) {
                            summary.append("\n");
                        }
                        summary.append("• ").append(msg.getActions().get(i).toReadableSummary());
                    }
                    tvActionsSummary.setText(summary.toString());

                    if (msg.isActionsResolved()) {
                        llActionButtons.setVisibility(View.GONE);
                        tvResolvedStatus.setVisibility(View.VISIBLE);
                        tvResolvedStatus.setText(msg.getResolutionStatusText());
                    } else {
                        llActionButtons.setVisibility(View.VISIBLE);
                        tvResolvedStatus.setVisibility(View.GONE);

                        btnApply.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int count = executeAiActions(msg.getActions());
                                msg.markResolved("Applied " + count + " change(s) to your schedule / To-Do!");
                                renderAllMessages();
                                Toast.makeText(requireContext(),
                                        "Changes applied!", Toast.LENGTH_SHORT).show();
                            }
                        });

                        btnDismiss.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                msg.markResolved("Dismissed");
                                renderAllMessages();
                            }
                        });
                    }
                } else {
                    cardActions.setVisibility(View.GONE);
                }
            }

            llChatMessages.addView(row);
        }

        // Прокручиваем ленту чата в самый низ к последнему сообщению
        scrollChat.post(new Runnable() {
            @Override
            public void run() {
                scrollChat.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * Выполняет список подтверждённых пользователем команд от ИИ в базе данных SQLite.
     */
    private int executeAiActions(List<AiAction> actions) {
        int applied = 0;
        for (AiAction action : actions) {
            if (AiAction.TYPE_ADD_TODO.equals(action.type)) {
                TodoItem newTodo = new TodoItem(
                        action.title,
                        action.durationMinutes,
                        action.prepMinutes,
                        action.category,
                        action.priority,
                        action.preferredTime,
                        action.deadlineDay,
                        action.repeatType,
                        action.targetCount,
                        -1,
                        -1
                );
                dbHelper.addTodoItem(newTodo);
                applied++;

            } else if (AiAction.TYPE_SCHEDULE_TODO.equals(action.type)) {
                TodoItem todo = action.todoId > 0
                        ? dbHelper.getTodoById(action.todoId)
                        : dbHelper.findTodoByTitle(action.title);

                if (todo != null) {
                    ScheduleEvent ev = new ScheduleEvent(
                            todo.getTitle(),
                            action.dayOfWeek,
                            action.startMinutes,
                            todo.getDurationMinutes(),
                            todo.getPrepMinutes(),
                            todo.getCategory(),
                            todo.getId(),
                            false
                    );
                    dbHelper.addScheduleEvent(ev);
                } else {
                    ScheduleEvent ev = new ScheduleEvent(
                            action.title,
                            action.dayOfWeek,
                            action.startMinutes,
                            action.durationMinutes,
                            action.prepMinutes,
                            action.category,
                            -1,
                            false
                    );
                    dbHelper.addScheduleEvent(ev);
                }
                applied++;

            } else if (AiAction.TYPE_ADD_EVENT.equals(action.type)) {
                ScheduleEvent ev = new ScheduleEvent(
                        action.title,
                        action.dayOfWeek,
                        action.startMinutes,
                        action.durationMinutes,
                        action.prepMinutes,
                        action.category,
                        -1,
                        false
                );
                dbHelper.addScheduleEvent(ev);
                applied++;

            } else if (AiAction.TYPE_MOVE_EVENT.equals(action.type)) {
                int targetEventId = action.eventId;
                if (targetEventId <= 0) {
                    ScheduleEvent found = dbHelper.findEventByTitle(action.title);
                    if (found != null) {
                        targetEventId = found.getId();
                    }
                }
                if (targetEventId > 0) {
                    dbHelper.updateEventTime(targetEventId, action.dayOfWeek, action.startMinutes);
                    applied++;
                }

            } else if (AiAction.TYPE_DELETE_EVENT.equals(action.type)) {
                int targetEventId = action.eventId;
                if (targetEventId <= 0) {
                    ScheduleEvent found = dbHelper.findEventByTitle(action.title);
                    if (found != null) {
                        targetEventId = found.getId();
                    }
                }
                if (targetEventId > 0) {
                    dbHelper.deleteScheduleEvent(targetEventId);
                    applied++;
                }
            }
        }
        return applied;
    }

    /**
     * Диалог сохранения бесплатного ключа Gemini API в локальные SharedPreferences.
     */
    private void showApiKeyDialog() {
        final EditText etKey = new EditText(requireContext());
        etKey.setHint("Paste Gemini API key (AIza...)");
        etKey.setText(prefs.getString(KEY_GEMINI_API, ""));
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        etKey.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(requireContext())
                .setTitle("Gemini API Key")
                .setMessage("Get a free API key at aistudio.google.com/apikey.\n"
                        + "It is saved locally on your phone (SharedPreferences) and never pushed to GitHub.")
                .setView(etKey)
                .setPositiveButton("Save Key", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String key = etKey.getText().toString().trim();
                        prefs.edit().putString(KEY_GEMINI_API, key).apply();
                        Toast.makeText(requireContext(), "API key saved!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
