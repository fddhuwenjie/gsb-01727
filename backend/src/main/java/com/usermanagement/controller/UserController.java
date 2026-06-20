package com.usermanagement.controller;

import com.usermanagement.dto.*;
import com.usermanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ApiResponse<LoginResponse.UserDTO> getProfile(Authentication authentication) {
        return ApiResponse.success(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ApiResponse<LoginResponse.UserDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ApiResponse.success("密码修改成功", null);
    }
}
