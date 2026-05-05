package com.teapack.processing.repository;

import com.teapack.processing.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository extends
        JpaRepository<AuditEvent, Long>,
        JpaSpecificationExecutor<AuditEvent> {
}
