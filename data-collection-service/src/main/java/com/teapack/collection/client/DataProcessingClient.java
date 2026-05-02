package com.teapack.collection.client;

import com.teapack.collection.dto.EquipmentReadingDto;
import com.teapack.collection.dto.OperatorEventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "data-processing-service", url = "${data-processing.url}")
public interface DataProcessingClient {

    @PostMapping("/api/processing/equipment")
    void sendEquipmentReading(@RequestBody EquipmentReadingDto dto);

    @PostMapping("/api/processing/operator-event")
    void sendOperatorEvent(@RequestBody OperatorEventDto dto);
}