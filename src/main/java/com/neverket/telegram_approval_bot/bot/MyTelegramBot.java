package com.neverket.telegram_approval_bot.bot;

import com.neverket.telegram_approval_bot.config.BotConfig;
import com.neverket.telegram_approval_bot.model.*;
import com.neverket.telegram_approval_bot.service.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class MyTelegramBot extends TelegramLongPollingBot {

    private BotConfig botConfig;
    private UserService userService;
    private RequestStatusService requestStatusService;
    private RequestService requestService;
    private UserStateService userStateService;
    private ApprovalRouteService approvalRouteService;


    @Override
    public String getBotUsername() {
        return botConfig.botUsername;
    }

    @Override
    public String getBotToken() {
        return botConfig.botToken;
    }

    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            System.out.println("\nТекст пользователя:\n" + messageText + "\n");

            Long telegramId = update.getMessage().getFrom().getId();
            String firstName = update.getMessage().getFrom().getFirstName();
            String lastName = update.getMessage().getFrom().getLastName();
            String userName = update.getMessage().getFrom().getUserName();

            // Получаем пользователя по его Telegram ID
            User user = userService.findByTelegramId(telegramId)
                    .orElseGet(() -> {
                        User newUser = new User(
                                telegramId,
                                false,
                                firstName,
                                lastName,
                                userName
                        );  // Если пользователя нет, создаем нового
                        System.out.println("1/ User not found, prepare to create and save");
                        return userService.saveUser(newUser);
                    });
            System.out.println("1/ User found or created");
            System.out.println(user.toString());

            Optional<UserState> userState = userStateService.findByUserId(user.getId());
            if (userState.isEmpty()) {
                System.out.println("2/ UserState not founded");
            } else {
                System.out.println(userState.get().toString());
            }

            switch (messageText) {
                case "/start":
                    startMessage(chatId, firstName);
                    break;

                case "/help":
                    sendHelpMessage(chatId);
                    break;

                case "/my_requests":
                    var requests = requestService.findAllByUser(user);
                    if (requests.isEmpty()) {
                        sendMessage(chatId, "У вас пока нет заявок.");
                    } else {
                        StringBuilder sb = new StringBuilder("Ваши заявки:\n\n");
                        for (Request req : requests) {
                            sb.append("• [").append(req.getStatus().getName()).append("] ")
                                    .append(req.getText()).append("\n");
                        }
                        sendMessage(chatId, sb.toString());
                    }
                    break;

                case "/new_request":
                    // Если пользователь не в процессе создания заявки
                    if (userState.isEmpty()) {
                        startNewRequestProcess(chatId, telegramId);
                    } else if (userState.get().getState().equals(UserBotState.NONE)) {
//                        userState.get().setState(UserBotState.NONE);
                        startNewRequestProcess(chatId, telegramId);
                    } else if (userState.get().getState().equals(UserBotState.WAITING_DESCRIPTION)
                            || userState.get().getState().equals(UserBotState.WAITING_APPROVERS_ON_LEVEL)
                    ) {
                        sendMessage(chatId, "Вы уже находитесь в процессе создания заявки.");
                    }
                    break;

                case "/done":
                    if (userState.isPresent() && userState.get().getState().equals(UserBotState.WAITING_APPROVERS_ON_LEVEL)) {
                        userState.get().setState(UserBotState.CONFIRMING_REQUEST);
                        userStateService.saveUserState(userState.get());
                        handleUserInput(chatId, user.getId(), messageText);
                    }
                    break;

                default:
                    if (userState.isPresent() && userState.get().getState() != UserBotState.NONE) {
                        handleUserInput(chatId, user.getId(), messageText);
                    }
                    break;
            }
        }
    }

    private void startNewRequestProcess(Long chatId, Long telegramId) {
        Optional<User> user = userService.findByTelegramId(telegramId);
        System.out.println("3/ User found");

        // По умолчанию новый запрос будет иметь статус "Новый"
        RequestStatus newRequestStatus = requestStatusService.findByName("NEW")
                .orElseThrow(() -> new RuntimeException("Статус 'NEW' не найден"));

        // Создаем заявку
        Request request = new Request(newRequestStatus, user.get(), "");
        requestService.saveRequest(request);

        // Устанавливаем у пользователя состояние "ожидания описания заявки"
        UserState userState = userStateService.findByUserId(user.get().getId())
                .orElse(new UserState(user.get(), UserBotState.WAITING_DESCRIPTION, request, 0));
        userState.setState(UserBotState.WAITING_DESCRIPTION);
        userState.setRequestInProgress(request);
        userState.setCurrentApprovalLevel(0);
        userStateService.saveUserState(userState);

        sendMessage(chatId, "Пожалуйста, введите описание заявки.");
    }

    private void handleUserInput(Long chatId, Long userId, String messageText) {
        System.out.println("4/ handleUserInput");
        Optional<UserState> userState = userStateService.findByUserId(userId);
        if (userState.isEmpty()) {
            return;
        }
        Request request = userState.get().getRequestInProgress();
        System.out.println("userState: " + userState.get().toString());

        switch (userState.get().getState()) {
            case WAITING_DESCRIPTION:
                System.out.println("5/ come to WAITING_DESCRIPTION");
                // Обрабатываем описание заявки
                request.setText(messageText);
                requestService.saveRequest(request);

                // Переходим к следующему шагу - получаем ревьюеров
                userState.get().setState(UserBotState.WAITING_APPROVERS_ON_LEVEL);
                var uss = userStateService.saveUserState(userState.get());
                System.out.println("after save: " + uss.toString());

                StringBuilder sendMessageText = new StringBuilder(
                        "Теперь введите ники ревьюеров для первого уровня согласования, через @ и разделённые пробелом.\n\n");
                sendMessageText.append(getReviewers());
                sendMessage(chatId, sendMessageText.toString());
                break;

            case WAITING_APPROVERS_ON_LEVEL:
                System.out.println("5/ come to WAITING_APPROVERS_LEVEL");
                System.out.println("messageText: '" + messageText + "'");

                // Разделяем строку на никнеймы, убираем лишние пробелы
                String[] tokens = messageText.trim().split("\\s+");
                Set<String> reviewerUserNames = Arrays.stream(tokens)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new)); // сохраняем порядок, убираем повторы

                if (reviewerUserNames.isEmpty()) {
                    sendMessage(chatId, "Вы не ввели ни одного ревьювера. Введите ники через пробел, например: `@user1 @user2`");
                    return;
                }

                List<User> validReviewers = new ArrayList<>();
                for (String dogUserName : reviewerUserNames) {
                    if (!dogUserName.startsWith("@")) {
                        sendMessage(chatId, "Ник '" + dogUserName + "' некорректен. Каждый ник должен начинаться с '@'. Повторите ввод.");
                        return;
                    }

                    String userName = dogUserName.substring(1);
                    Optional<User> reviewerOpt = userService.findByUserName(userName);

                    if (reviewerOpt.isEmpty()) {
                        sendMessage(chatId, "Ревьювер @" + userName + " не найден в системе. Повторите ввод.");
                        return;
                    }

                    User reviewer = reviewerOpt.get();
                    if (!reviewer.isReviewer()) {
                        sendMessage(chatId, "Пользователь @" + userName + " не является ревьювером. Повторите ввод.");
                        return;
                    }

                    validReviewers.add(reviewer);
                }

                // Все ревьюверы прошли проверку — можно сохранять
                Request requestInProgress = userState.get().getRequestInProgress();
                int currentLevel = userState.get().getCurrentApprovalLevel() + 1;

                for (User reviewer : validReviewers) {
                    var approvalRoute = new ApprovalRoute(
                            requestInProgress,
                            currentLevel,
                            reviewer,
                            ApprovalStatus.PENDING
                    );
                    approvalRouteService.save(approvalRoute);
                }

                userState.get().setCurrentApprovalLevel(currentLevel);
                userStateService.saveUserState(userState.get());


                sendMessage(chatId, "Введите ревьюеров для " + (currentLevel + 1) + " уровня согласования.\n"
                        + "Или введите /done для завершения.");
                break;

//            case CONFIRMING_REQUEST:
//                System.out.println("7/ come to CONFIRMING_REQUEST");
//                // Если заявка готова, подтверждаем её
//                sendMessage(chatId, "Заявка готова, ожидайте подтверждения.");
//                break;

            case CONFIRMING_REQUEST:
                System.out.println("7/ come to CONFIRMING_REQUEST");
                if (request == null) {
                    sendMessage(chatId, "Ошибка: заявка не найдена.");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("Ваша заявка готова:\n\n");
                sb.append("ID: ").append(request.getId()).append("\n");
                sb.append("Текст: ").append(request.getText()).append("\n");
                sb.append("Статус: ").append(request.getStatus().getName()).append("\n\n");

                List<ApprovalRoute> routes = approvalRouteService.findByRequestOrderByLevel(request);

                if (routes.isEmpty()) {
                    sb.append("Маршрут согласования не задан.\n");
                } else {
                    sb.append("Маршрут согласования:\n");

                    Map<Integer, List<String>> reviewersByLevel = new TreeMap<>();
                    for (ApprovalRoute route : routes) {
                        reviewersByLevel
                                .computeIfAbsent(route.getLevel(), k -> new ArrayList<>())
                                .add("@" + route.getReviewer().getUserName() + " (" + route.getApprovalStatus().name() + ")");
                    }

                    for (var entry : reviewersByLevel.entrySet()) {
                        sb.append("  Уровень ").append(entry.getKey()).append(": ");
                        sb.append(String.join(", ", entry.getValue()));
                        sb.append("\n");
                    }
                }

                sb.append("\nЗаявка сохранена. Ожидайте подтверждения от ревьюверов.");
                sendMessage(chatId, sb.toString());

                // Завершаем процесс создания заявки
                userState.get().setState(UserBotState.NONE);
                userState.get().setRequestInProgress(null);
                userState.get().setCurrentApprovalLevel(0);
                userStateService.saveUserState(userState.get());
                break;

            default:
                System.out.println("8/ come to default");
                break;
        }
    }

    void startMessage(Long chatId, String firstName) {
        String answer = "Привет, " + firstName + "! Вот что я умею:\n\n" +
                "/start – запуск бота\n" +
                "/new_request – новая заявка\n" +
                "/my_requests – просмотр своих заявок\n" +
                "/approve – одобрение заявки\n" +
                "/reject – отклонение заявки\n" +
                "/help – справка по боту";
        sendMessage(chatId, answer);
    }

    private void sendHelpMessage(Long chatId) {
        String answer = "Справка по боту:\n\n" +
                "/start – запуск бота\n" +
                "/new_request – новая заявка\n" +
                "/my_requests – просмотр своих заявок\n" +
                "/approve – одобрение заявки\n" +
                "/reject – отклонение заявки\n" +
                "/help – справка по боту";
        sendMessage(chatId, answer);
    }

    void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getReviewers() {
        StringBuilder reviewersList = new StringBuilder("Список доступных ревьюеров:\n");
        List<User> availableReviewers = userService.findAllReviewers();
        for (int i = 0; i < availableReviewers.size(); i++) {
            reviewersList.append((i + 1) + ". @" + availableReviewers.get(i).getUserName()).append("\n");
        }
        return reviewersList.toString();
    }

}

