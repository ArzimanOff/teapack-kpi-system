package com.teapack.reporting.repository;

import com.teapack.reporting.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByShiftId(Long shiftId);
    List<Report> findByLineIdOrderByCreatedAtDesc(String lineId);
}