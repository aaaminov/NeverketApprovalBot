package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.ApprovalRoute;
import com.neverket.telegram_approval_bot.model.ApprovalStatus;
import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApprovalRouteService {

    private final ApprovalRouteRepository approvalRouteRepository;
    private final NotificationService notificationService;

//    public ApprovalRoute save(ApprovalRoute approvalRoute) {
//        return approvalRouteRepository.save(approvalRoute);
//    }

//    public ApprovalRoute save(ApprovalRoute route) {
//        ApprovalRoute saved = approvalRouteRepository.save(route);
//        notificationService.notifyApprovers(route.getRequest());
//        return saved;
//    }

    public ApprovalRoute save(ApprovalRoute route) {
        return approvalRouteRepository.save(route);
    }

    public List<ApprovalRoute> findByRequest(Request request) {
        return approvalRouteRepository.findByRequest(request);
    }

    public List<ApprovalRoute> findByRequestOrderByLevel(Request request) {
        return approvalRouteRepository.findByRequestOrderByLevelAsc(request);
    }

    public Optional<ApprovalRoute> findByRequestIdAndReviewer(Long requestId, User reviewer) {
        return approvalRouteRepository.findByRequestIdAndReviewer(requestId, reviewer);
    }

    public Optional<ApprovalRoute> findFirstPendingByRequestIdAndReviewer(Long requestId, User reviewer) {
        return approvalRouteRepository.findByRequestIdAndReviewerAndApprovalStatus(
                requestId,
                reviewer,
                ApprovalStatus.PENDING
        ).stream().findFirst();
    }

    public void saveAll(List<ApprovalRoute> routes) {
        approvalRouteRepository.saveAll(routes);
    }
}
