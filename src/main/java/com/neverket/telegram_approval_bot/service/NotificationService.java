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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ApprovalRouteRepository approvalRouteRepository;
    private final MessageSender messageSender;

    public void notifyApprovers(Request request) {
        // Получаем все маршруты для текущей заявки
        List<ApprovalRoute> routes = approvalRouteRepository.findByRequest(request);

        // Группируем по уровням
        routes.stream()
                .collect(Collectors.groupingBy(ApprovalRoute::getLevel))
                .forEach((level, levelRoutes) -> {
                    // Для каждого уровня находим тех, кто должен одобрять
                    levelRoutes.stream()
                            .filter(route -> route.getApprovalStatus() == ApprovalStatus.PENDING)
                            .forEach(route -> {
                                User reviewer = route.getReviewer();
                                sendApprovalNotification(reviewer, request, level);
                            });
                });
    }

    private void sendApprovalNotification(User reviewer, Request request, int level) {
        String message = String.format(
                "🔔 Новая заявка на согласование (Уровень %d):\n\n" +
                        "ID: %d\n" +
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

    public void notifyRequester(Request request, User reviewer, ApprovalStatus status) {
        String statusMessage = switch (status) {
            case APPROVED -> "одобрена";
            case REJECTED -> "отклонена";
            case CHANGES_REQUESTED -> "требует доработки";
            default -> "изменена";
        };

        String message = String.format(
                "📢 Статус вашей заявки #%d изменен:\n\n" +
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
}
