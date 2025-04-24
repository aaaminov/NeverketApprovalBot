package com.neverket.telegram_approval_bot.model;

public enum ApprovalStatus {
    PENDING,           // Ожидает решения
    APPROVED,          // Одобрено
    REJECTED,          // Отклонено
    CHANGES_REQUESTED  // Требует доработки
}
