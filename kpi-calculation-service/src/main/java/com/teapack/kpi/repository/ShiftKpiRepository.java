package com.teapack.kpi.repository;

import com.teapack.kpi.entity.ShiftKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface ShiftKpiRepository extends JpaRepository<ShiftKpi, Long>, JpaSpecificationExecutor<ShiftKpi> {
    Optional<ShiftKpi> findByShiftId(Long shiftId);
    List<ShiftKpi> findByLineIdOrderByCalculatedAtDesc(String lineId);
}
