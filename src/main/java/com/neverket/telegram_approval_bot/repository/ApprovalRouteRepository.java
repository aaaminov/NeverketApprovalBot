package com.neverket.telegram_approval_bot.repository;

import com.neverket.telegram_approval_bot.model.ApprovalRoute;
import com.neverket.telegram_approval_bot.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRouteRepository extends JpaRepository<ApprovalRoute, Long> {
//    Optional<ApprovalRoute> save(ApprovalRoute approvalRoute);
    List<ApprovalRoute> findByRequestOrderByLevel(Request request);

}
