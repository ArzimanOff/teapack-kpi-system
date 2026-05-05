package com.teapack.emulator.service;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmulatorRunState {
    private Long shiftId;
    private String lineId;
    private String status;        // RUNNING / STOPPED
    private LocalDateTime startedAt;

    private double nominalSpeed;
    private double minSpeed;
    private double maxSpeed;
    private double minTemp;
    private double maxTemp;

    /** Активный сценарий: NORMAL / SPEED_DROP / SCRAP_BURST / OUTLIER */
    private String scenario = "NORMAL";

    /** Сколько следующих тиков применять сценарий (0 = больше не применять) */
    private int scenarioTicksLeft = 0;

    private long ticks = 0;
}
