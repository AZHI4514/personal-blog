package com.azhi.service.impl;

import com.azhi.mapper.UserMapper;
import com.azhi.pojo.User;
import com.azhi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User register(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.selectByEmail(user.getEmail()) != null) {
            throw new IllegalArgumentException("邮箱已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        User user = userMapper.selectByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }
}
