package com.teapack.kpi.client;

import com.teapack.kpi.dto.KpiResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${notification.url}")
public interface NotificationClient {

    @PostMapping("/api/notifications/check")
    void checkKpiThresholds(@RequestBody KpiResultDto kpiResult);
}