package com.teapack.collection.repository;

import com.teapack.collection.entity.EquipmentReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface EquipmentReadingRepository extends JpaRepository<EquipmentReading, Long> {

    List<EquipmentReading> findByShiftId(Long shiftId);

    List<EquipmentReading> findByLineIdAndTimestampBetween(
            String lineId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT AVG(e.lineSpeed) FROM EquipmentReading e WHERE e.shiftId = :shiftId AND e.status = 'RUNNING'")
    Double findAverageSpeedByShiftId(@Param("shiftId") Long shiftId);

    @Query("SELECT SUM(e.outputCount) FROM EquipmentReading e WHERE e.shiftId = :shiftId")
    Integer findTotalOutputByShiftId(@Param("shiftId") Long shiftId);
}