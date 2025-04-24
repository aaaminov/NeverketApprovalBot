package com.neverket.telegram_approval_bot.bot;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class MessageSender {

    private MyTelegramBot bot;

    public void setBot(MyTelegramBot bot) {
        this.bot = bot;
    }

    public void sendStartMessage(Long chatId, String firstName) {
        String answer = "Привет, " + firstName + "! Вот что я умею:\n\n" +
                "/start – запуск бота\n" +
                "/new_request – новая заявка\n" +
                "/my_requests – просмотр своих заявок\n" +
                "/approve – одобрение заявки\n" +
                "/reject – отклонение заявки\n" +
                "/help – справка по боту";
        sendMessage(chatId, answer);
    }

    public void sendHelpMessage(Long chatId) {
        String answer = "Справка по боту:\n\n" +
                "/start – запуск бота\n" +
                "/new_request – новая заявка\n" +
                "/my_requests – просмотр своих заявок\n" +
                "/help – справка по боту\n" +
                "/become_reviewer – стать ревьювером\n" +
                "/remove_reviewer – перестать быть ревьювером\n" +
//                "/approve_[id] - Одобрить заявку\n" +
//                "/reject_[id] - Отклонить заявку\n" +
//                "/request_changes_[id] - Запросить доработку\n" +

                "";
        sendMessage(chatId, answer);
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageWithButtons(Long chatId, String text, List<InlineKeyboardButton> buttons) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        if (!buttons.isEmpty()) {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(buttons));
            message.setReplyMarkup(markup);
        }

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

