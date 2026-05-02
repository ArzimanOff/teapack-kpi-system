package com.teapack.processing.repository;

import com.teapack.processing.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findByLineIdAndStatus(String lineId, String status);
    List<Shift> findByLineIdOrderByCreatedAtDesc(String lineId);
    List<Shift> findByStatus(String status);
}