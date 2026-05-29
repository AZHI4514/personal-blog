package com.azhi.controller;

import com.azhi.pojo.Result;
import com.azhi.pojo.User;
import com.azhi.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user, HttpSession session) {
        User registeredUser = userService.register(user);
        session.setAttribute("currentUser", registeredUser);
        return Result.success(registeredUser);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> body, HttpSession session) {
        User user = userService.login(body.get("username"), body.get("password"));
        session.setAttribute("currentUser", user);
        return Result.success(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }
}
