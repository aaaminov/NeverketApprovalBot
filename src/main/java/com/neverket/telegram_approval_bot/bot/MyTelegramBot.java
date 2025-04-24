package com.neverket.telegram_approval_bot.bot;

import com.neverket.telegram_approval_bot.config.BotConfig;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.model.UserBotState;
import com.neverket.telegram_approval_bot.model.UserState;
import com.neverket.telegram_approval_bot.service.*;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
@AllArgsConstructor
public class MyTelegramBot extends TelegramLongPollingBot {

    private BotConfig botConfig;
    private UserService userService;
    private RequestStatusService requestStatusService;
    private RequestService requestService;
    private UserStateService userStateService;
    private ApprovalRouteService approvalRouteService;
    private MessageSender messageSender;
    private CommandProcessor commandProcessor;

    @PostConstruct
    public void init() {
        messageSender.setBot(this);
    }

    @Override
    public String getBotUsername() {
        return botConfig.botUsername;
    }

    @Override
    public String getBotToken() {
        return botConfig.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        MessageContext context = createMessageContext(update);
        logUserInfo(context);

        User user = getUserOrCreateIfNotExists(context);
        Optional<UserState> userState = userStateService.findByUserId(user.getId());

        processMessage(context, user, userState);
    }

    private MessageContext createMessageContext(Update update) {
        return MessageContext.builder()
                .chatId(update.getMessage().getChatId())
                .messageText(update.getMessage().getText())
                .telegramId(update.getMessage().getFrom().getId())
                .firstName(update.getMessage().getFrom().getFirstName())
                .lastName(update.getMessage().getFrom().getLastName())
                .userName(update.getMessage().getFrom().getUserName())
                .build();
    }

    private void logUserInfo(MessageContext context) {
        System.out.println("\nТекст пользователя:\n" + context.getMessageText() + "\n");
    }

    private User getUserOrCreateIfNotExists(MessageContext context) {
        return userService.findByTelegramId(context.getTelegramId())
                .orElseGet(() -> createNewUser(context));
    }

    private User createNewUser(MessageContext context) {
        User newUser = new User(
                context.getTelegramId(),
                false,
                context.getFirstName(),
                context.getLastName(),
                context.getUserName()
        );
        System.out.println("1/ User not found, prepare to create and save");
        return userService.saveUser(newUser);
    }

    private void processMessage(MessageContext context, User user, Optional<UserState> userState) {
        if (userState.isPresent()) {
            System.out.println(userState.get().toString());
        } else {
            System.out.println("2/ UserState not founded");
        }

        if (isCommand(context.getMessageText())) {
            commandProcessor.processCommand(context, user, userState);
        } else if (userState.isPresent() && userState.get().getState() != UserBotState.NONE) {
            commandProcessor.handleUserInput(context, user.getId(), userState.get());
        }
    }

    private boolean isCommand(String messageText) {
        return messageText.startsWith("/");
    }

}

