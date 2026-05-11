package com.teapack.kpi.controller;

import com.teapack.kpi.dto.RecommendationDto;
import com.teapack.kpi.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','TECHNOLOGIST','ADMIN')")
    public ResponseEntity<List<RecommendationDto>> getRecommendations(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String lineId,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(recommendationService.recommend(role, lineId, days));
    }
}
