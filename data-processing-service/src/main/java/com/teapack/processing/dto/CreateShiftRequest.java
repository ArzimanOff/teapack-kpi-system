package com.teapack.processing.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateShiftRequest {

    @NotBlank
    private String lineId;

    private Long operatorId;

    @NotNull
    private LocalDateTime plannedStart;

    @NotNull
    private LocalDateTime plannedEnd;

    @NotNull @Min(0)
    private Integer plannedOutput;

    @NotNull
    private BigDecimal nominalSpeed;
}