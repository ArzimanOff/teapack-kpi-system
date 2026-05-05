package com.teapack.notification.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class KpiThresholdsDto {

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal oeeMin;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal availabilityMin;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal performanceMin;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("1.0")
    private BigDecimal qualityMin;
}
