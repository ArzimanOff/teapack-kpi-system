package com.teapack.processing.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperatorEventDto {
    private Long shiftId;
    private String lineId;
    private LocalDateTime timestamp;
    private String eventType;
    private String reason;
    private Integer scrapCount;
    private String comment;
    private Long operatorId;
}