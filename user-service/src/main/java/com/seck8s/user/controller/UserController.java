package com.seck8s.user.controller;

import com.seck8s.common.context.UserContext;
import com.seck8s.common.result.Result;
import com.seck8s.user.entity.User;
import com.seck8s.user.service.UserService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword(),
                request.getPhone(), request.getRealName(), request.getIdCard());
        return Result.ok(user);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return Result.ok(new LoginResponse(token));
    }

    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = UserContext.requireUserId();
        return Result.ok(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody User updatedInfo) {
        Long userId = UserContext.requireUserId();
        userService.updateProfile(userId, updatedInfo);
        return Result.ok();
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String phone;
        private String realName;
        private String idCard;
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class LoginResponse {
        private String token;

        LoginResponse(String token) {
            this.token = token;
        }
    }
}
