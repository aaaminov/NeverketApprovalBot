package com.neverket.telegram_approval_bot.repository;

import com.neverket.telegram_approval_bot.model.Request;
import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.model.UserState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStateRepository extends JpaRepository<UserState, Long> {
    Optional<UserState> findByUserId(Long userId);
    Optional<UserState> findByUserAndRequestInProgress(User user, Request request);
}
