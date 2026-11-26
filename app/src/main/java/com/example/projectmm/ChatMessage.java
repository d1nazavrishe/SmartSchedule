package com.example.projectmm;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель одного сообщения в экране чата с ИИ.
 * Может содержать обычный текст, а для ответов ИИ — ещё и список предложенных действий (AiAction),
 * которые пользователь может подтвердить кнопкой "Apply".
 */
public class ChatMessage {

    private String text;                    // Текст сообщения
    private boolean fromUser;               // true = сообщение от пользователя, false = от ИИ
    private List<AiAction> actions;         // Предложенные ИИ изменения в расписании / To-Do
    private boolean actionsResolved;        // Нажал ли пользователь уже Apply или Dismiss
    private String resolutionStatusText;    // Статус после нажатия ("Applied!" или "Dismissed")

    public ChatMessage(String text, boolean fromUser) {
        this.text = text;
        this.fromUser = fromUser;
        this.actions = new ArrayList<>();
        this.actionsResolved = false;
        this.resolutionStatusText = "";
    }

    public ChatMessage(String text, boolean fromUser, List<AiAction> actions) {
        this.text = text;
        this.fromUser = fromUser;
        this.actions = actions != null ? actions : new ArrayList<>();
        this.actionsResolved = false;
        this.resolutionStatusText = "";
    }

    public String getText() {
        return text;
    }

    public boolean isFromUser() {
        return fromUser;
    }

    public List<AiAction> getActions() {
        return actions;
    }

    public boolean hasPendingActions() {
        return actions != null && !actions.isEmpty() && !actionsResolved;
    }

    public boolean isActionsResolved() {
        return actionsResolved;
    }

    public void markResolved(String statusText) {
        this.actionsResolved = true;
        this.resolutionStatusText = statusText;
    }

    public String getResolutionStatusText() {
        return resolutionStatusText;
    }
}
