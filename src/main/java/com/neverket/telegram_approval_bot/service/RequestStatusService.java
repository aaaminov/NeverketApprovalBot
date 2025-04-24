package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.RequestStatus;
import com.neverket.telegram_approval_bot.repository.RequestStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RequestStatusService {

    private final RequestStatusRepository statusRepository;

    @Autowired
    public RequestStatusService(RequestStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public Optional<RequestStatus> findByName(String name) {
        return statusRepository.findByName(name);
    }
}
