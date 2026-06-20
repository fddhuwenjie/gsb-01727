package com.usermanagement.config;

import com.usermanagement.entity.User;
import com.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("管理员");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setNickname("普通用户");
            user.setRole(User.Role.USER);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
}
