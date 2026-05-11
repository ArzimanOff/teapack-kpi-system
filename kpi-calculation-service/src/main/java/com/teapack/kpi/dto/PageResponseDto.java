package com.teapack.kpi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Минималистичная обёртка под Spring's Page<T>, чтобы Feign смог распарсить.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponseDto<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
}
