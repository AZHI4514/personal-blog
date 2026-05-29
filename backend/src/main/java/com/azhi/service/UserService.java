package com.azhi.service;

import com.azhi.pojo.User;

public interface UserService {
    User register(User user);

    User login(String username, String password);
}
