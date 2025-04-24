package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.ApprovalRoute;
import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.repository.ApprovalRouteRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApprovalRouteService {

    private final ApprovalRouteRepository approvalRouteRepository;

    @Autowired
    public ApprovalRouteService(ApprovalRouteRepository approvalRouteRepository) {
        this.approvalRouteRepository = approvalRouteRepository;
    }

    public List<ApprovalRoute> findByRequestOrderByLevel(Request request) {
        return approvalRouteRepository.findByRequestOrderByLevel(request);
    }

    public ApprovalRoute save(ApprovalRoute approvalRoute) {
        return approvalRouteRepository.save(approvalRoute);
    }

}
