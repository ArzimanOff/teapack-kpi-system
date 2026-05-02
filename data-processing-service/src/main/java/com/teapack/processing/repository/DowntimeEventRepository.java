package com.teapack.processing.repository;

import com.teapack.processing.entity.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {
    List<DowntimeEvent> findByShiftIdOrderByStartTimeAsc(Long shiftId);

    @Query("SELECT d FROM DowntimeEvent d WHERE d.shiftId = :shiftId AND d.endTime IS NULL")
    Optional<DowntimeEvent> findOpenDowntimeByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT COALESCE(SUM(d.durationMinutes), 0) FROM DowntimeEvent d WHERE d.shiftId = :shiftId")
    java.math.BigDecimal findTotalDowntimeByShiftId(@Param("shiftId") Long shiftId);
}