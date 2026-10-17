package com.seck8s.user.service;

import com.seck8s.user.entity.User;

public interface UserService {
    User register(String username, String password, String phone, String realName, String idCard);

    String login(String username, String password);

    User getProfile(Long userId);

    void updateProfile(Long userId, User updatedInfo);
}
