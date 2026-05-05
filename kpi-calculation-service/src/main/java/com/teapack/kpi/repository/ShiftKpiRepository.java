package com.teapack.kpi.repository;

import com.teapack.kpi.entity.ShiftKpi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShiftKpiRepository extends JpaRepository<ShiftKpi, Long>, JpaSpecificationExecutor<ShiftKpi> {
    Optional<ShiftKpi> findByShiftId(Long shiftId);
    List<ShiftKpi> findByLineIdOrderByCalculatedAtDesc(String lineId);

    @Query("""
        SELECT
          COUNT(k),
          AVG(k.oee), AVG(k.availability), AVG(k.performance), AVG(k.quality),
          COALESCE(SUM(k.totalOutput), 0),
          COALESCE(SUM(k.goodOutput), 0),
          COALESCE(SUM(k.scrapCount), 0),
          COALESCE(SUM(k.downtime), 0),
          COALESCE(SUM(k.numberOfStops), 0),
          MIN(k.calculatedAt),
          MAX(k.calculatedAt)
        FROM ShiftKpi k
        WHERE k.lineId = :lineId
          AND (:from IS NULL OR k.calculatedAt >= :from)
          AND (:to IS NULL OR k.calculatedAt <= :to)
    """)
    Object[] aggregateByLine(@Param("lineId") String lineId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    List<ShiftKpi> findByLineIdAndCalculatedAtBetweenOrderByCalculatedAtDesc(
            String lineId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<ShiftKpi> findByLineIdOrderByCalculatedAtDesc(String lineId, Pageable pageable);
}
