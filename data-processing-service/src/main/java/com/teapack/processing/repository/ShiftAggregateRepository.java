package com.teapack.processing.repository;

import com.teapack.processing.entity.ShiftAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShiftAggregateRepository extends JpaRepository<ShiftAggregate, Long> {
    Optional<ShiftAggregate> findByShiftId(Long shiftId);
}