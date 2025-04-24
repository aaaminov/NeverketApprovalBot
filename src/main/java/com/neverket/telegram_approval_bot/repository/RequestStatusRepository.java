package com.neverket.telegram_approval_bot.repository;

import com.neverket.telegram_approval_bot.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestStatusRepository extends JpaRepository<RequestStatus, Integer> {
    Optional<RequestStatus> findByName(String name);
}