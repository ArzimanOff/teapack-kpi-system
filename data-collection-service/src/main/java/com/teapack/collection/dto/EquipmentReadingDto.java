package com.teapack.collection.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EquipmentReadingDto {

    @NotBlank
    private String lineId;

    @NotNull
    private LocalDateTime timestamp;

    @DecimalMin("0.0") @DecimalMax("200.0")
    private BigDecimal temperature;

    @DecimalMin("0.0") @DecimalMax("1000.0")
    private BigDecimal lineSpeed;

    @NotBlank
    private String status; // RUNNING, STOPPED, IDLE

    @Min(0)
    private Integer outputCount;

    private Long shiftId;
}