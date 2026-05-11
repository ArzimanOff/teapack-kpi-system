package com.teapack.reporting.service;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Загружает TTF-шрифт с кириллицей для PDF.
 * Сначала пытается прочитать путь из конфига, затем — известные системные пути
 * (Windows / Linux Docker). Если ничего не нашли — fallback на встроенный
 * Helvetica (кириллица заменится на пустые квадраты, но PDF сгенерируется).
 */
@Slf4j
@Component
public class PdfFontProvider {

    private static final List<String> FALLBACK_REGULAR = List.of(
            "C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"
    );

    private static final List<String> FALLBACK_BOLD = List.of(
            "C:/Windows/Fonts/arialbd.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/liberation/LiberationSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
    );

    @Value("${report.font-path:}")
    private String configuredRegular;

    @Value("${report.font-path-bold:}")
    private String configuredBold;

    private BaseFont regular;
    private BaseFont bold;

    @PostConstruct
    void init() {
        regular = loadFirstAvailable(configuredRegular, FALLBACK_REGULAR);
        bold = loadFirstAvailable(configuredBold, FALLBACK_BOLD);
        if (regular == null) {
            log.warn("Cyrillic TTF font not found. PDF will fallback to Helvetica (no Cyrillic).");
        } else {
            log.info("PDF font loaded: regular ok={}, bold ok={}", true, bold != null);
        }
    }

    public Font regular(float size) { return font(regular, size, Font.NORMAL); }
    public Font bold(float size)    { return font(bold != null ? bold : regular, size, Font.BOLD); }
    public Font italic(float size)  { return font(regular, size, Font.ITALIC); }

    private Font font(BaseFont bf, float size, int style) {
        if (bf == null) return new Font(Font.HELVETICA, size, style);
        return new Font(bf, size, style);
    }

    private BaseFont loadFirstAvailable(String configured, List<String> fallbacks) {
        if (configured != null && !configured.isBlank() && new File(configured).isFile()) {
            BaseFont bf = tryLoad(configured);
            if (bf != null) return bf;
        }
        for (String path : fallbacks) {
            if (new File(path).isFile()) {
                BaseFont bf = tryLoad(path);
                if (bf != null) return bf;
            }
        }
        return null;
    }

    private BaseFont tryLoad(String path) {
        try {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            log.warn("Failed to load TTF '{}': {}", path, e.getMessage());
            return null;
        }
    }
}
