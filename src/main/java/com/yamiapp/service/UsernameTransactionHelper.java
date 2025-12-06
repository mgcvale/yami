package com.yamiapp.service;

import com.yamiapp.model.User;
import com.yamiapp.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsernameTransactionHelper {
    private final UserRepository repo;

    public UsernameTransactionHelper(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public User updateUsername(User u, String username) {
        u.setUsername(username);
        return repo.save(u);
    }
}
