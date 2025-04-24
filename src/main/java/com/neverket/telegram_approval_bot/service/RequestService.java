package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.*;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;
import com.neverket.telegram_approval_bot.repository.RequestRepository;
import com.neverket.telegram_approval_bot.repository.RequestStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
            request.setStatus(requestStatusService.findByName("APPROVED").orElseThrow());
            requestRepository.save(request);
            notificationService.notifyAllParticipants(
                    request,
                    "Заявка #" + request.getId() + " успешно согласована. ✅"
            );
        } else if (anyRejected) {
            request.setStatus(requestStatusService.findByName("REJECTED").orElseThrow());
            requestRepository.save(request);
//            notificationService.notifyRequester(request, reviewer, ApprovalStatus.REJECTED);
            notificationService.notifyAllParticipants(
                    request,
                    "Заявка #" + request.getId() + " отклонена. Процесс согласования прекращен. ❌"
            );
            return;
        } else if (anyChangesRequested) {
            request.setStatus(requestStatusService.findByName("NEEDS_REVISION").orElseThrow());
        } else {
            request.setStatus(requestStatusService.findByName("IN_REVIEW").orElseThrow());
            // Уведомляем следующих ревьюверов, если заявка еще в процессе
            notificationService.notifyNextReviewers(request);
        }

        requestRepository.save(request);
    }
}

