package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.RequestStatus;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestService {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request saveRequest(Request request) {
        return requestRepository.save(request);
    }

    public List<Request> findAllByUser(User user) {
        return requestRepository.findByUser(user);
    }
}

