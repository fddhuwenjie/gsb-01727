package com.usermanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {
    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$|^$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    private String role;

    private Boolean enabled;
}
