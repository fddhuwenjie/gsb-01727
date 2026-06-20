package com.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;

    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;
}
