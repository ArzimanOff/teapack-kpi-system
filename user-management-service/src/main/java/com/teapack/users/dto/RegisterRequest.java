package com.teapack.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private String fullName;
    private String email;
    @NotBlank
    private String role; // ROLE_OPERATOR, ROLE_TECHNOLOGIST, ROLE_ADMIN
}