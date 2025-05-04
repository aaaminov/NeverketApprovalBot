package com.neverket.telegram_approval_bot.model;

public enum ApprovalStatus {
    PENDING {
        @Override
        public String toString() {
            return "Ожидает решения";
        }
    },           // Ожидает решения
    APPROVED {
        @Override
        public String toString() {
            return "Одобрено";
        }
    },          // Одобрено
    REJECTED {
        @Override
        public String toString() {
            return "Отклонено";
        }
    },          // Отклонено
    CHANGES_REQUESTED {
        @Override
        public String toString() {
            return "Требует доработки";
        }
    }  // Требует доработки
}
