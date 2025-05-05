package com.neverket.telegram_approval_bot.bot;

import com.neverket.telegram_approval_bot.model.*;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;
import com.neverket.telegram_approval_bot.service.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class CommandProcessor {

    public static final String NEW_REQUEST_STATUS = "NEW";
    public static final String COMMAND_START = "/start";
    public static final String COMMAND_HELP = "/help";
    public static final String COMMAND_MY_REQUESTS = "/my_requests";
    public static final String COMMAND_NEW_REQUEST = "/new_request";
    public static final String COMMAND_DONE = "/done";

    private UserService userService;
    private RequestStatusService requestStatusService;
    private RequestService requestService;
    private UserStateService userStateService;
    private ApprovalRouteService approvalRouteService;
    private MessageSender messageSender;
    private final NotificationService notificationService;
    private final ApprovalRouteRepository approvalRouteRepository;

    // commands

    public void processCommand(MessageContext context, User user, Optional<UserState> userState) {
        switch (context.getMessageText()) {
            // v1
            case COMMAND_START:
                messageSender.sendStartMessage(context.getChatId(), context.getFirstName());
                break;
            case COMMAND_HELP:
                messageSender.sendHelpMessage(context.getChatId());
                break;
            case COMMAND_MY_REQUESTS:
                handleMyRequestsCommand(context.getChatId(), user);
                break;
            case COMMAND_NEW_REQUEST:
                handleNewRequestCommand(context.getChatId(), user, userState);
                break;
            case COMMAND_DONE:
                handleDoneCommand(context, user, userState);  // Передаем context здесь
                break;
            // v2
            case String cmd when cmd.startsWith("/approve_"):
                handleApproveCommand(context.getChatId(), user, cmd);
                break;
            case String cmd when cmd.startsWith("/reject_"):
                handleRejectCommand(context.getChatId(), user, cmd);
                break;
            case String cmd when cmd.startsWith("/request_changes_"):
                handleRequestChangesCommand(context.getChatId(), user, cmd);
                break;
            // v3
            case "/become_reviewer":
                handleBecomeReviewer(context.getChatId(), user);
                break;
            case "/remove_reviewer":
                handleRemoveReviewer(context.getChatId(), user);
                break;
            // v4
            case "/requests":
                handleRequestsCommand(context.getChatId(), user);
                break;
            default:
                break;
        }
    }

    // // v1
    private void handleMyRequestsCommand(Long chatId, User user) {
        var requests = requestService.findAllByUser(user);
        if (requests.isEmpty()) {
            messageSender.sendMessage(chatId, "У вас пока нет заявок.");
        } else {
            StringBuilder sb = new StringBuilder("Ваши заявки:\n\n");
            for (Request req : requests) {
                sb.append("# ").append(req.getId()).append("\n");
                sb.append("Статус: ").append(req.getStatus().getName()).append("\n");
                sb.append("Текст:\n");
                sb.append(req.getText()).append("\n");

                List<ApprovalRoute> routes = approvalRouteService.findByRequestOrderByLevel(req);
                if (!routes.isEmpty()) {
                    sb.append("Процесс согласования:\n");
                    routes.stream()
                            .collect(Collectors.groupingBy(ApprovalRoute::getLevel))
                            .forEach((level, levelRoutes) -> {
                                sb.append("  Уровень ").append(level).append(":\n");
                                levelRoutes.forEach(route -> {
                                    sb.append("    — @").append(route.getReviewer().getUserName()).append(" ")
                                            .append(route.getApprovalStatus().toString()).append("\n");

                                });
                            });
                }
                sb.append("\n");
            }
            messageSender.sendMessage(chatId, sb.toString());
        }
    }

    private void handleNewRequestCommand(Long chatId, User user, Optional<UserState> userState) {
        if (userState.isEmpty() || userState.get().getState().equals(UserBotState.NONE)) {
            startNewRequestProcess(chatId, user);
        } else if (userState.get().getState().equals(UserBotState.WAITING_DESCRIPTION)
                || userState.get().getState().equals(UserBotState.WAITING_APPROVERS_ON_LEVEL)) {
            messageSender.sendMessage(chatId, "Вы уже находитесь в процессе создания заявки.");
        }
    }

    private void handleDoneCommand(MessageContext context, User user, Optional<UserState> userState) {
        if (userState.isPresent() && userState.get().getState().equals(UserBotState.WAITING_APPROVERS_ON_LEVEL)) {
            userState.get().setState(UserBotState.CONFIRMING_REQUEST);
            userStateService.saveUserState(userState.get());
            handleUserInput(context, user.getId(), userState.get());
        }
    }

    // // v2 - buttons
    public void handleApproveCommand(Long chatId, User user, String callbackData) {
        long requestId = Long.parseLong(callbackData.substring("approve_".length()));
        updateApprovalStatus(chatId, user, requestId, ApprovalStatus.APPROVED);
    }

    public void handleRejectCommand(Long chatId, User user, String callbackData) {
        long requestId = Long.parseLong(callbackData.substring("reject_".length()));
        updateApprovalStatus(chatId, user, requestId, ApprovalStatus.REJECTED);
    }

    public void handleRequestChangesCommand(Long chatId, User user, String callbackData) {
        long requestId = Long.parseLong(callbackData.substring("request_changes_".length()));
        updateApprovalStatus(chatId, user, requestId, ApprovalStatus.CHANGES_REQUESTED);
    }

    // // v3
    private void handleBecomeReviewer(Long chatId, User user) {
        if (user.isReviewer()) {
            messageSender.sendMessage(chatId, "Вы уже являетесь ревьювером");
            return;
        }

        user.setReviewer(true);
        userService.saveUser(user);
        messageSender.sendMessage(chatId, "Теперь вы ревьювер.\n"
                + "Теперь вы можете участвовать в согласовании заявок");
    }

    private void handleRemoveReviewer(Long chatId, User user) {
        if (!user.isReviewer()) {
            messageSender.sendMessage(chatId, "Вы не являетесь ревьювером");
            return;
        }

        user.setReviewer(false);
        userService.saveUser(user);
        messageSender.sendMessage(chatId, "Вы больше не ревьювер\n"
                + "Теперь вы не будете получать заявки на согласование");
    }


    private void startNewRequestProcess(Long chatId, User user) {
        RequestStatus newRequestStatus = requestStatusService.findByName(NEW_REQUEST_STATUS)
                .orElseThrow(() -> new RuntimeException("Статус 'NEW' не найден"));

        Request request = new Request(newRequestStatus, user, "");
        requestService.saveRequest(request);

        UserState userState = userStateService.findByUserId(user.getId())
                .orElse(new UserState(user, UserBotState.WAITING_DESCRIPTION, request, 0));
        userState.setState(UserBotState.WAITING_DESCRIPTION);
        userState.setRequestInProgress(request);
        userState.setCurrentApprovalLevel(0);
        userStateService.saveUserState(userState);

        messageSender.sendMessage(chatId, "Пожалуйста, введите текст заявки.");
    }


    // user input

    public void handleUserInput(MessageContext context, Long userId, UserState userState) {
        System.out.println("4/ handleUserInput");
        Request request = userState.getRequestInProgress();
        System.out.println("userState: " + userState.toString());

        switch (userState.getState()) {
            case WAITING_DESCRIPTION:
                handleWaitingDescriptionState(context, request, userState);
                break;
            case WAITING_APPROVERS_ON_LEVEL:
                handleWaitingApproversState(context, request, userState);
                break;
            case CONFIRMING_REQUEST:
                handleConfirmingRequestState(context, request, userState);
                break;
            case EDITING_REQUEST:
                handleEditingRequestState(context, userState);
                break;
            default:
                System.out.println("8/ come to default");
                break;
        }
    }

    private void handleWaitingDescriptionState(MessageContext context, Request request, UserState userState) {
        System.out.println("5/ come to WAITING_DESCRIPTION");
        request.setText(context.getMessageText());
        requestService.saveRequest(request);

        userState.setState(UserBotState.WAITING_APPROVERS_ON_LEVEL);
        var savedState = userStateService.saveUserState(userState);
        System.out.println("after save: " + savedState.toString());

        StringBuilder messageText = new StringBuilder(
                "Теперь введите ники ревьюеров для первого уровня согласования, через @ и разделённые пробелом.\n\n");
        messageText.append(getReviewers());
        messageSender.sendMessage(context.getChatId(), messageText.toString());
    }

    private void handleWaitingApproversState(MessageContext context, Request request, UserState userState) {
        System.out.println("5/ come to WAITING_APPROVERS_LEVEL");
        System.out.println("messageText: '" + context.getMessageText() + "'");

        Set<String> reviewerUserNames = extractReviewerUsernames(context.getMessageText());
        if (reviewerUserNames.isEmpty()) {
            messageSender.sendMessage(context.getChatId(),
                    "Вы не ввели ни одного ревьювера. Введите ники через пробел, например: `@user1 @user2`");
            return;
        }

        List<User> validReviewers = validateReviewers(context.getChatId(), reviewerUserNames, request);
        if (validReviewers == null) return;

        saveApprovalRoutes(request, userState, validReviewers);
        notifyNextApprovalLevel(context.getChatId(), userState.getCurrentApprovalLevel() + 1);
    }

    private Set<String> extractReviewerUsernames(String messageText) {
        return Arrays.stream(messageText.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<User> validateReviewers(Long chatId, Set<String> reviewerUserNames, Request request) {
        List<User> validReviewers = new ArrayList<>();

        for (String dogUserName : reviewerUserNames) {
            if (!dogUserName.startsWith("@")) {
                messageSender.sendMessage(chatId,
                        "Ник '" + dogUserName + "' некорректен. Каждый ник должен начинаться с '@'. Повторите ввод.");
                return null;
            }

            String userName = dogUserName.substring(1);
            Optional<User> reviewerOpt = userService.findByUserName(userName);

            if (reviewerOpt.isEmpty()) {
                messageSender.sendMessage(chatId, "Ревьювер @" + userName + " не найден в системе. Повторите ввод.");
                return null;
            }

            User reviewer = reviewerOpt.get();
            if (!reviewer.isReviewer()) {
                messageSender.sendMessage(chatId,
                        "Пользователь @" + userName + " не является ревьювером. Повторите ввод.");
                return null;
            }

            // Проверка на уникальность в рамках заявки
            if (isReviewerAlreadyInRequest(request, reviewer)) {
                messageSender.sendMessage(chatId,
                        "Ревьювер @" + userName + " уже добавлен в другой уровень.\n" +
                                "Один пользователь не может быть ревьювером на нескольких уровнях.");
                return null;
            }

            validReviewers.add(reviewer);
        }

        return validReviewers;
    }

    private boolean isReviewerAlreadyInRequest(Request request, User reviewer) {
        return approvalRouteRepository.existsByRequestAndReviewer(request, reviewer);
    }

    private void saveApprovalRoutes(Request request, UserState userState, List<User> validReviewers) {
        int currentLevel = userState.getCurrentApprovalLevel() + 1;

        for (User reviewer : validReviewers) {
            var approvalRoute = new ApprovalRoute(
                    request,
                    currentLevel,
                    reviewer,
                    ApprovalStatus.PENDING
            );
            approvalRouteService.save(approvalRoute);
        }

        userState.setCurrentApprovalLevel(currentLevel);
        userStateService.saveUserState(userState);
    }

    private void notifyNextApprovalLevel(Long chatId, int nextLevel) {
        messageSender.sendMessage(chatId, "Введите ревьюеров для " + nextLevel + " уровня согласования.\n"
                + "Или введите /done для завершения.");
    }

    private void handleConfirmingRequestState(MessageContext context, Request request, UserState userState) {
        System.out.println("7/ come to CONFIRMING_REQUEST");
        if (request == null) {
            messageSender.sendMessage(context.getChatId(), "Ошибка: заявка не найдена.");
            return;
        }

        String requestSummary = buildRequestSummary(request);
        messageSender.sendMessage(context.getChatId(), requestSummary);

        completeRequestCreation(userState);
    }

    private String buildRequestSummary(Request request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ваша заявка готова:\n\n");
        sb.append("#").append(request.getId()).append("\n");
        sb.append("Текст: ").append(request.getText()).append("\n");
        sb.append("Статус: ").append(request.getStatus().getName()).append("\n\n");

        List<ApprovalRoute> routes = approvalRouteService.findByRequestOrderByLevel(request);
        if (!routes.isEmpty()) {
            sb.append("Маршрут согласования:\n");
            Map<Integer, List<String>> reviewersByLevel = groupReviewersByLevel(routes);
            appendReviewersInfo(sb, reviewersByLevel);
        } else {
            sb.append("Маршрут согласования не задан.\n");
        }

        sb.append("\nЗаявка сохранена. Ожидайте подтверждения от ревьюверов.");
        return sb.toString();
    }

    private Map<Integer, List<String>> groupReviewersByLevel(List<ApprovalRoute> routes) {
        Map<Integer, List<String>> reviewersByLevel = new TreeMap<>();
        for (ApprovalRoute route : routes) {
            reviewersByLevel
                    .computeIfAbsent(route.getLevel(), k -> new ArrayList<>())
                    .add("@" + route.getReviewer().getUserName() + " (" + route.getApprovalStatus().name() + ")");
        }
        return reviewersByLevel;
    }

    private void appendReviewersInfo(StringBuilder sb, Map<Integer, List<String>> reviewersByLevel) {
        for (var entry : reviewersByLevel.entrySet()) {
            sb.append("  Уровень ").append(entry.getKey()).append(": ");
            sb.append(String.join(", ", entry.getValue()));
            sb.append("\n");
        }
    }

    private void completeRequestCreation(UserState userState) {
        // Устанавливаем специальный статус для готовой к согласованию заявки
        RequestStatus readyStatus = requestStatusService.findByName("PENDING_APPROVAL")
                .orElseThrow(() -> new RuntimeException("Status not found"));

        Request request = userState.getRequestInProgress();
        request.setStatus(readyStatus);
        requestService.saveRequest(request);

        // Отправляем уведомления ревьюверам
        notificationService.notifyApprovers(request);

        // Сбрасываем состояние пользователя
        userState.setState(UserBotState.NONE);
        userState.setRequestInProgress(null);
        userState.setCurrentApprovalLevel(0);
        userStateService.saveUserState(userState);
    }

    private String getReviewers() {
        StringBuilder reviewersList = new StringBuilder("Список доступных ревьюеров:\n");
        List<User> availableReviewers = userService.findAllReviewers();
        for (int i = 0; i < availableReviewers.size(); i++) {
            reviewersList.append((i + 1) + ". @" + availableReviewers.get(i).getUserName()).append("\n");
        }
        return reviewersList.toString();
    }


    private void updateApprovalStatus(Long chatId, User reviewer, long requestId, ApprovalStatus status) {
        Optional<ApprovalRoute> routeOpt = approvalRouteService.findFirstPendingByRequestIdAndReviewer(requestId, reviewer);

        if (routeOpt.isEmpty()) {
            messageSender.sendMessage(chatId, "Заявка не найдена или у вас нет прав для её согласования");
            return;
        }

        ApprovalRoute route = routeOpt.get();
        route.setApprovalStatus(status);
        approvalRouteService.save(route);

        if (status == ApprovalStatus.CHANGES_REQUESTED) {
            Request request = route.getRequest();

            // Получаем все маршруты согласования
            List<ApprovalRoute> allRoutes = approvalRouteService.findByRequestOrderByLevel(request);

            // Находим минимальный уровень с PENDING
            Optional<Integer> minPendingLevel = allRoutes.stream()
                    .filter(r -> r.getApprovalStatus() == ApprovalStatus.PENDING ||
                            r.getApprovalStatus() == ApprovalStatus.CHANGES_REQUESTED)
                    .map(ApprovalRoute::getLevel)
                    .min(Integer::compare);


            if (minPendingLevel.isPresent()) {
                System.out.println("minPendingLevel: " + minPendingLevel);

                int currentLevel = minPendingLevel.get();
                resetApprovalsForLevel(request, currentLevel);

                if (currentLevel > 1) {
                    // Находим предыдущий уровень
                    int previousLevel = currentLevel - 1;

                    // Сбрасываем все APPROVED статусы предыдущего уровня
                    resetApprovalsForLevel(request, previousLevel);

                    // Уведомляем предыдущий уровень
                    notificationService.notifyReviewersOnLevel(request, previousLevel);
                } else {
                    // Сбрасываем первый уровень
                    resetApprovalsForLevel(request, 1);
                    notificationService.notifyReviewersOnLevel(request, 1);
                }
            } else {
                // Если все уровни завершены, сбрасываем последний
                int maxLevel = allRoutes.stream()
                        .mapToInt(ApprovalRoute::getLevel)
                        .max()
                        .orElse(1);

                System.out.println("minPendingLevel not found, maxLevel: " + maxLevel);

                resetApprovalsForLevel(request, maxLevel);
                notificationService.notifyReviewersOnLevel(request, maxLevel);
            }

            request.setStatus(requestStatusService.findByName("NEEDS_REVISION").orElseThrow());
            requestService.saveRequest(request);

            // Устанавливаем состояние редактирования
            UserState userState = userStateService.findByUserId(request.getUser().getId())
                    .orElse(new UserState(request.getUser(), UserBotState.NONE, null, 0));

            userState.setState(UserBotState.EDITING_REQUEST);
            userState.setRequestInProgress(request);
            userStateService.saveUserState(userState);

            messageSender.sendMessage(
                    request.getUser().getTelegramId(),
                    "Запрошена доработка заявки #" + requestId +
                            "\nОт ревьювера: @" + reviewer.getUserName() +
                            "\nПредыдущий текст:\n" + request.getText() +
                            "\n\nВведите новый текст заявки:"
            );
        } else {
            requestService.updateRequestStatusAfterApproval(requestId, notificationService);
        }
    }

    private void resetApprovalsForLevel(Request request, int level) {
        List<ApprovalRoute> levelRoutes = approvalRouteRepository.findByRequestAndLevel(request, level);
        levelRoutes.forEach(route -> {
            if (route.getApprovalStatus() == ApprovalStatus.APPROVED) {
                route.setApprovalStatus(ApprovalStatus.PENDING);
            }
        });
        approvalRouteService.saveAll(levelRoutes);
    }

    private void handleEditingRequestState(MessageContext context, UserState userState) {
        Request request = userState.getRequestInProgress();

        // Обновляем текст
        request.setText(context.getMessageText());

        // Сбрасываем статус заявки
        RequestStatus pendingStatus = requestStatusService.findByName("PENDING_APPROVAL")
                .orElseThrow(() -> new RuntimeException("Статус PENDING_APPROVAL не найден"));
        request.setStatus(pendingStatus);

        // Сбрасываем статусы всех ApprovalRoute
        List<ApprovalRoute> routes = approvalRouteService.findByRequest(request);
        routes.forEach(route -> {
            if (route.getApprovalStatus() != ApprovalStatus.APPROVED) {
                route.setApprovalStatus(ApprovalStatus.PENDING);
            }
        });
        approvalRouteService.saveAll(routes);

        // Сохраняем изменения
        requestService.saveRequest(request);

        // Сбрасываем состояние пользователя
        userState.setState(UserBotState.NONE);
        userState.setRequestInProgress(null);
        userStateService.saveUserState(userState);

        // Уведомляем автора
        messageSender.sendMessage(
                context.getChatId(),
                "Заявка обновлена и снова отправлена на согласование."
        );

        // Отправляем уведомления ревьюверам
        notificationService.notifyApprovers(request);
    }


    private void handleRequestsCommand(Long chatId, User reviewer) {

        List<ApprovalRoute> approvalRoutes = approvalRouteService.findByReviewerAndApprovalStatus(reviewer, ApprovalStatus.PENDING);

        if (approvalRoutes.isEmpty()) {
            messageSender.sendMessage(chatId, "Нет активных заявок на согласование.");
            return;
        }

        for (ApprovalRoute ar : approvalRoutes) {
            Request request = ar.getRequest();
            messageSender.sendMessage(chatId, "Текущие заявки вам на согласование:");
            notificationService.sendApprovalNotificationWithButtons(reviewer, request);
        }

    }
}

