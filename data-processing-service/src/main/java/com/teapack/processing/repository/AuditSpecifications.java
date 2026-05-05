package com.teapack.processing.repository;

import com.teapack.processing.entity.AuditEvent;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuditSpecifications {
    private AuditSpecifications() {}

    public static Specification<AuditEvent> build(
            String actor, String action, String targetType, String targetId,
            LocalDateTime from, LocalDateTime to
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                ps.add(cb.equal(root.get("actor"), actor));
            }
            if (action != null && !action.isBlank()) {
                ps.add(cb.equal(root.get("action"), action));
            }
            if (targetType != null && !targetType.isBlank()) {
                ps.add(cb.equal(root.get("targetType"), targetType));
            }
            if (targetId != null && !targetId.isBlank()) {
                ps.add(cb.equal(root.get("targetId"), targetId));
            }
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
