package com.usermanagement.controller;

import com.usermanagement.dto.*;
import com.usermanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ApiResponse<List<LoginResponse.UserDTO>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ApiResponse<LoginResponse.UserDTO> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping("/users")
    public ApiResponse<LoginResponse.UserDTO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<LoginResponse.UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success(userService.updateUser(id, request));
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return ApiResponse.success("密码重置成功", null);
    }

    @PostMapping("/users/{id}/toggle-status")
    public ApiResponse<LoginResponse.UserDTO> toggleUserStatus(@PathVariable Long id) {
        return ApiResponse.success(userService.toggleUserStatus(id));
    }
}
