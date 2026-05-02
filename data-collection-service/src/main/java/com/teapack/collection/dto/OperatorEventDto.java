package com.teapack.collection.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperatorEventDto {

    @NotNull
    private Long shiftId;

    @NotBlank
    private String lineId;

    @NotNull
    private LocalDateTime timestamp;

    @NotBlank
    private String eventType; // START, STOP, DOWNTIME, SCRAP

    private String reason;

    @Min(0)
    private Integer scrapCount;

    private String comment;

    private Long operatorId;
}