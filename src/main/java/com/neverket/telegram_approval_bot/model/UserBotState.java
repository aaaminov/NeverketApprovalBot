package com.neverket.telegram_approval_bot.model;

public enum UserBotState {
    NONE, // по умолчанию
    WAITING_DESCRIPTION,
    WAITING_APPROVERS_ON_LEVEL,
    CONFIRMING_REQUEST,
    EDITING_REQUEST
}
