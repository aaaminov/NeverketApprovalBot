package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.bot.MessageSender;
import com.neverket.telegram_approval_bot.model.ApprovalRoute;
import com.neverket.telegram_approval_bot.model.ApprovalStatus;
import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ApprovalRouteRepository approvalRouteRepository;
    private final MessageSender messageSender;
    private final RequestStatusService requestStatusService;
    private final RequestService requestService;

//    public void notifyApprovers(Request request) {
//        // Проверяем, что заявка в финальном состоянии
//        if (!request.getStatus().getName().equals("PENDING_APPROVAL")) {
//            return;
//        }
//
//        List<ApprovalRoute> routes = approvalRouteRepository.findByRequestOrderByLevelAsc(request);
//
//        // Находим первый уровень, где есть PENDING статусы
//        routes.stream()
//                .filter(route -> route.getApprovalStatus() == ApprovalStatus.PENDING)
//                .collect(Collectors.groupingBy(ApprovalRoute::getLevel))
//                .entrySet().stream()
//                .min(Map.Entry.comparingByKey()) // Берем минимальный уровень
//                .ifPresent(entry -> {
//                    entry.getValue().forEach(route -> {
//                        sendApprovalNotificationWithButtons(route.getReviewer(), request);
//                    });
//                });
//    }

    public void notifyApprovers(Request request) {
        // Находим первый уровень с PENDING статусами
        approvalRouteRepository.findByRequest(request)
                .stream()
                .filter(route -> route.getApprovalStatus() == ApprovalStatus.PENDING)
                .collect(Collectors.groupingBy(ApprovalRoute::getLevel))
                .entrySet()
                .stream()
                .min(Map.Entry.comparingByKey())
                .ifPresent(entry -> entry.getValue().forEach(route -> {
                    sendApprovalNotificationWithButtons(route.getReviewer(), request);
                }));
    }

    private void sendApprovalNotification(User reviewer, Request request, int level) {
        String message = String.format(
                "Новая заявка на согласование (Уровень %d):\n\n" +
                        "#%d\n" +
                        "Автор: @%s\n" +
                        "Текст: %s\n\n" +
                        "Команды:\n" +
                        "/approve_%d - Одобрить\n" +
                        "/reject_%d - Отклонить\n" +
                        "/request_changes_%d - Запросить доработку",
                level,
                request.getId(),
                request.getUser().getUserName(),
                request.getText(),
                request.getId(),
                request.getId(),
                request.getId()
        );

        messageSender.sendMessage(reviewer.getTelegramId(), message);
    }

    private void sendApprovalNotificationWithButtons(User reviewer, Request request) {
        String message = String.format(
                "Вам заявка на согласование:\n\n" +
                        "#%d\n" +
                        "Автор: @%s\n" +
                        "Текст: %s",
                request.getId(),
                request.getUser().getUserName(),
                request.getText()
        );

        List<InlineKeyboardButton> buttons = Arrays.asList(
                createButton("Одобрить", "approve_" + request.getId()),
                createButton("Отклонить", "reject_" + request.getId()),
                createButton("Доработка", "request_changes_" + request.getId())
        );

        messageSender.sendMessageWithButtons(reviewer.getTelegramId(), message, buttons);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(text);
        button.setCallbackData(callbackData);
        return button;
    }

    public void notifyRequester(Request request, User reviewer, ApprovalStatus status) {
        String statusMessage = switch (status) {
            case APPROVED -> "одобрена";
            case REJECTED -> "отклонена";
            case CHANGES_REQUESTED -> "требует доработки";
            default -> "изменена";
        };

        String message = String.format(
                "Статус вашей заявки #%d изменен:\n\n" +
                        "Ревьювер: @%s\n" +
                        "Новый статус: %s\n" +
                        "Текст заявки: %s",
                request.getId(),
                reviewer.getUserName(),
                statusMessage,
                request.getText()
        );

        messageSender.sendMessage(request.getUser().getTelegramId(), message);
    }

    public void notifyNextReviewers(Request request) {
        // Находим текущий минимальный уровень с PENDING статусами
        Optional<Integer> currentMinLevel = approvalRouteRepository
                .findByRequest(request)
                .stream()
                .filter(route -> route.getApprovalStatus() == ApprovalStatus.PENDING)
                .map(ApprovalRoute::getLevel)
                .min(Integer::compare);

        if (currentMinLevel.isEmpty()) {
            // Все уровни пройдены
            request.setStatus(requestStatusService.findByName("APPROVED").orElseThrow());
            requestService.saveRequest(request);
            return;
        }

        // Находим следующий уровень (текущий минимальный + 1)
        int nextLevel = currentMinLevel.get() + 1;

        // Получаем всех ревьюверов следующего уровня
        List<ApprovalRoute> nextLevelRoutes = approvalRouteRepository
                .findByRequestAndLevel(request, nextLevel);

        if (!nextLevelRoutes.isEmpty()) {
            nextLevelRoutes.forEach(route -> {
                if (route.getApprovalStatus() == ApprovalStatus.PENDING) {
                    sendApprovalNotificationWithButtons(route.getReviewer(), request);
                }
            });
        } else {
            // Если следующего уровня нет - заявка завершена
            request.setStatus(requestStatusService.findByName("APPROVED").orElseThrow());
            requestService.saveRequest(request);
        }
    }

    public void notifyAllParticipants(Request request, String message) {
        // Уведомляем автора
        messageSender.sendMessage(request.getUser().getTelegramId(), message);

        // Уведомляем всех ревьюверов
        approvalRouteRepository.findByRequest(request).forEach(route -> {
            messageSender.sendMessage(route.getReviewer().getTelegramId(), message);
        });
    }

    public void notifyReviewersOnLevel(Request request, int level) {
        approvalRouteRepository.findByRequestAndLevel(request, level)
                .forEach(route -> {
                    if (route.getApprovalStatus() == ApprovalStatus.PENDING) {
                        sendApprovalNotificationWithButtons(route.getReviewer(), request);
                    }
                });
    }
}
