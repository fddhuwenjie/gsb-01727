package com.usermanagement.service;

import com.usermanagement.dto.*;
import com.usermanagement.entity.User;
import com.usermanagement.repository.UserRepository;
import com.usermanagement.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.default-password}")
    private String defaultPassword;

    public LoginResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, LoginResponse.UserDTO.fromEntity(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponse(token, LoginResponse.UserDTO.fromEntity(user));
    }

    public LoginResponse.UserDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return LoginResponse.UserDTO.fromEntity(user);
    }

    @Transactional
    public LoginResponse.UserDTO updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);
        return LoginResponse.UserDTO.fromEntity(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次密码输入不一致");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Admin methods
    public List<LoginResponse.UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(LoginResponse.UserDTO::fromEntity)
                .toList();
    }

    public LoginResponse.UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return LoginResponse.UserDTO.fromEntity(user);
    }

    @Transactional
    public LoginResponse.UserDTO createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        String password = request.getPassword() != null ? request.getPassword() : defaultPassword;
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        userRepository.save(user);
        return LoginResponse.UserDTO.fromEntity(user);
    }

    @Transactional
    public LoginResponse.UserDTO updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(User.Role.valueOf(request.getRole()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);
        return LoginResponse.UserDTO.fromEntity(user);
    }

    @Transactional
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(defaultPassword));
        userRepository.save(user);
    }

    @Transactional
    public LoginResponse.UserDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        return LoginResponse.UserDTO.fromEntity(user);
    }
}
