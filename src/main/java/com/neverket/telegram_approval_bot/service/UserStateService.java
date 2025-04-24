package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.model.UserState;
import com.neverket.telegram_approval_bot.repository.UserStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserStateService {

    private final UserStateRepository userStateRepository;

    @Autowired
    public UserStateService(UserStateRepository userStateRepository) {
        this.userStateRepository = userStateRepository;
    }

    public Optional<UserState> findByUserId(Long userId) {
        return userStateRepository.findByUserId(userId);
    }

    public Optional<UserState> findByUserAndRequest(User user, Request request) {
        return userStateRepository.findByUserAndRequestInProgress(user, request);
    }

    public UserState saveUserState(UserState userState) {
        return userStateRepository.save(userState);
    }

}
