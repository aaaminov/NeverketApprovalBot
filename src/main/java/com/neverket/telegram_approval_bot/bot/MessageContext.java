package com.neverket.telegram_approval_bot.bot;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageContext {
    private Long chatId;
    private String messageText;
    private Long telegramId;
    private String firstName;
    private String lastName;
    private String userName;
}
