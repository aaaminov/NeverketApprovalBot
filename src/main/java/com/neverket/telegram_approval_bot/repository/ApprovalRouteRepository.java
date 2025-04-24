package com.neverket.telegram_approval_bot.repository;

import com.neverket.telegram_approval_bot.model.ApprovalRoute;
import com.neverket.telegram_approval_bot.model.ApprovalStatus;
import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalRouteRepository extends JpaRepository<ApprovalRoute, Long> {
//    Optional<ApprovalRoute> save(ApprovalRoute approvalRoute);
    List<ApprovalRoute> findByRequestOrderByLevel(Request request);

    List<ApprovalRoute> findByRequest(Request request);
    List<ApprovalRoute> findByRequestOrderByLevelAsc(Request request);
    Optional<ApprovalRoute> findByRequestIdAndReviewer(Long requestId, User reviewer);
    List<ApprovalRoute> findByRequestIdAndReviewerAndApprovalStatus(
            Long requestId,
            User reviewer,
            ApprovalStatus status
    );

    List<ApprovalRoute> findByRequestAndLevel(Request request, Integer integer);
    boolean existsByRequestAndReviewer(Request request, User reviewer);

    List<ApprovalRoute> findByRequestAndLevel( Request request, int level );
}
