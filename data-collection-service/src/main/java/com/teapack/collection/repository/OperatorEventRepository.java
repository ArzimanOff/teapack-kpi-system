package com.teapack.collection.repository;

import com.teapack.collection.entity.OperatorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OperatorEventRepository extends JpaRepository<OperatorEvent, Long> {

    List<OperatorEvent> findByShiftIdOrderByTimestampAsc(Long shiftId);

    List<OperatorEvent> findByShiftIdAndEventType(Long shiftId, String eventType);

    @Query("SELECT SUM(o.scrapCount) FROM OperatorEvent o WHERE o.shiftId = :shiftId")
    Integer findTotalScrapByShiftId(@Param("shiftId") Long shiftId);
}