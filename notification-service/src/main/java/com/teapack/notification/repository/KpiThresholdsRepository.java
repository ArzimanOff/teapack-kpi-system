package com.teapack.notification.repository;

import com.teapack.notification.entity.KpiThresholds;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KpiThresholdsRepository extends JpaRepository<KpiThresholds, Long> {
}
