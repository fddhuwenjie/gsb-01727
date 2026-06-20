package com.usermanagement.dto;

import com.usermanagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserDTO user;

    @Data
    public static class UserDTO {
        private Long id;
        private String username;
        private String nickname;
        private String phone;
        private String email;
        private String role;
        private Boolean enabled;

        public static UserDTO fromEntity(User user) {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().name());
            dto.setEnabled(user.getEnabled());
            return dto;
        }
    }
}
