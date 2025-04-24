package com.neverket.telegram_approval_bot.service;

import com.neverket.telegram_approval_bot.model.User;
import com.neverket.telegram_approval_bot.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public List<User> findAllReviewers() {
        return userRepository.findByIsReviewerTrue();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User getOrCreateUser(Long telegramId, String firstName, String lastName, String userName) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> userRepository.save(new User(
                        telegramId, false, firstName, lastName, userName
                )));
    }

//    public User findOrCreateTelegramUser(Long telegramId, String firstName, String lastName, String userName) {
//        return findByTelegramId(telegramId).orElseGet(() -> {
//            User newUser = new User(
//                    telegramId,
//                    false,
//                    firstName,
//                    lastName,
//                    userName
//            );
//            return saveUser(newUser);
//        });
//    }

}

