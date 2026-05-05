package com.teapack.users.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String role;        // ROLE_ADMIN | ROLE_OPERATOR | ROLE_TECHNOLOGIST
    private Boolean enabled;
    private String password;    // если задан — будет переустановлен
}
