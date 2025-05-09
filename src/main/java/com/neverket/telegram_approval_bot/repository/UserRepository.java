package com.neverket.telegram_approval_bot.repository;

import com.neverket.telegram_approval_bot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
    Optional<User> findByUserName(String userName);
    List<User> findByIsReviewerTrue();
}