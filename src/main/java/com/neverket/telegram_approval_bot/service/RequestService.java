package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.*;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;
import com.neverket.telegram_approval_bot.repository.RequestRepository;
import com.neverket.telegram_approval_bot.repository.RequestStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
//    private final NotificationService notificationService;
    private final ApprovalRouteRepository approvalRouteRepository;
    private final RequestStatusService requestStatusService;

    public Request saveRequest(Request request) {
        return requestRepository.save(request);
    }

    public List<Request> findAllByUser(User user) {
        return requestRepository.findByUser(user);
    }

    /**
     * Проверяет статусы всех ApprovalRoute и обновляет общий статус заявки
     */
    public void updateRequestStatusAfterApproval(Long requestId, NotificationService notificationService) {
        Request request = requestRepository.findById(requestId).orElseThrow();
        List<ApprovalRoute> routes = approvalRouteRepository.findByRequest(request);

        boolean allApproved = routes.stream()
                .allMatch(route -> route.getApprovalStatus() == ApprovalStatus.APPROVED);

        boolean anyRejected = routes.stream()
                .anyMatch(route -> route.getApprovalStatus() == ApprovalStatus.REJECTED);

        boolean anyChangesRequested = routes.stream()
                .anyMatch(route -> route.getApprovalStatus() == ApprovalStatus.CHANGES_REQUESTED);

        if (allApproved) {
            handleFullApproval(request, notificationService);
        } else if (anyRejected) {
            handleRejection(request, notificationService);
        } else if (anyChangesRequested) {
            handleChangesRequested(request);
        } else {
            handleInReview(request, notificationService);
        }

        requestRepository.save(request);
    }

    private void handleFullApproval(Request request, NotificationService notificationService) {
        request.setStatus(requestStatusService.findByName("APPROVED").orElseThrow());
        notificationService.notifyAllParticipants(
                request,
                "Заявка #" + request.getId() + " успешно согласована. ✅"
        );
    }

    private void handleRejection(Request request, NotificationService notificationService) {
        request.setStatus(requestStatusService.findByName("REJECTED").orElseThrow());
        notificationService.notifyAllParticipants(
                request,
                "Заявка #" + request.getId() + " отклонена. Процесс согласования прекращен. ❌"
        );
    }

    private void handleChangesRequested(Request request) {
        request.setStatus(requestStatusService.findByName("NEEDS_REVISION").orElseThrow());
    }

    private void handleInReview(Request request, NotificationService notificationService) {
        request.setStatus(requestStatusService.findByName("IN_REVIEW").orElseThrow());

        // Находим текущий активный уровень
        Optional<Integer> currentLevel = approvalRouteRepository.findByRequest(request)
                .stream()
                .filter(r -> r.getApprovalStatus() == ApprovalStatus.PENDING)
                .map(ApprovalRoute::getLevel)
                .min(Integer::compare);

        if (currentLevel.isPresent()) {
            // Уведомляем всех на текущем уровне
            notificationService.notifyReviewersOnLevel(request, currentLevel.get());
        } else {
            // Если уровней нет - автоматическое одобрение
            handleFullApproval(request, notificationService);
        }
    }
}

