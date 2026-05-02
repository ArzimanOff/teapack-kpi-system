package com.teapack.users.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}