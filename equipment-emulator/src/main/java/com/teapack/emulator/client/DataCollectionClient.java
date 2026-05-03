package com.teapack.emulator.client;

import com.teapack.emulator.dto.EquipmentReadingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "data-collection-service", url = "${data-collection.url}")
public interface DataCollectionClient {

    @PostMapping("/api/collect/equipment")
    Object sendReading(@RequestBody EquipmentReadingDto dto);
}