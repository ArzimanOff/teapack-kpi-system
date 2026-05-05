package com.teapack.processing.service;

import com.teapack.processing.entity.AuditEvent;
import com.teapack.processing.repository.AuditEventRepository;
import com.teapack.processing.repository.AuditSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;

    /**
     * Запись аудит-события. REQUIRES_NEW гарантирует, что лог сохранится
     * даже если основная транзакция упадёт после.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String targetType, Object targetId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : "system";
        String role = auth != null && auth.getAuthorities() != null
                ? auth.getAuthorities().stream().findFirst()
                    .map(GrantedAuthority::getAuthority).orElse(null)
                : null;
        try {
            repository.save(AuditEvent.builder()
                    .actor(actor)
                    .actorRole(role)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId != null ? String.valueOf(targetId) : null)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit event {}/{}: {}", action, targetId, e.getMessage());
        }
    }

    public Page<AuditEvent> find(String actor, String action, String targetType,
                                 String targetId, LocalDateTime from, LocalDateTime to,
                                 Pageable pageable) {
        return repository.findAll(
                AuditSpecifications.build(actor, action, targetType, targetId, from, to),
                pageable
        );
    }
}
