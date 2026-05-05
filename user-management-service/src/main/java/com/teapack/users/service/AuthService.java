package com.teapack.users.service;

import com.teapack.users.config.JwtUtil;
import com.teapack.users.dto.*;
import com.teapack.users.entity.User;
import com.teapack.users.repository.RoleRepository;
import com.teapack.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()
                )
        );
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String role = user.getRole().getName();
        String access = jwtUtil.generateAccessToken(user.getUsername(), role);
        String refresh = jwtUtil.generateRefreshToken(user.getUsername(), role);
        return new LoginResponse(access, refresh, user.getUsername(), role);
    }

    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        if (!JwtUtil.TYPE_REFRESH.equals(jwtUtil.extractType(refreshToken))) {
            throw new RuntimeException("Provided token is not a refresh token");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("User is disabled");
        }
        String role = user.getRole().getName();
        String newAccess = jwtUtil.generateAccessToken(username, role);
        // Сами refresh-токены оставляем тем же — single-use ротацию делать не будем для простоты ВКР
        return new LoginResponse(newAccess, refreshToken, username, role);
    }

    public UserDto register(RegisterRequest request) {
        // Открытая регистрация разрешена только когда в системе ещё нет пользователей
        // (первичный bootstrap-админ). После этого новые учётки создаёт ROLE_ADMIN
        // через POST /api/users.
        if (userRepository.count() > 0) {
            throw new SecurityException(
                    "Open registration disabled: создание пользователей доступно только администратору");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        var role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(role)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getName());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}