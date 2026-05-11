package com.teapack.reporting.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.teapack.reporting.client.KpiClient;
import com.teapack.reporting.client.ProcessingClient;
import com.teapack.reporting.dto.DowntimeEventDto;
import com.teapack.reporting.dto.KpiResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Color HEADER_BG = new Color(0x40, 0x60, 0x9c);
    private static final Color ROW_ALT = new Color(0xF5, 0xF7, 0xFA);

    private final KpiClient kpiClient;
    private final ProcessingClient processingClient;
    private final PdfFontProvider fonts;

    public byte[] renderShiftReport(Long shiftId) {
        KpiResultDto k = kpiClient.getKpiByShift(shiftId);
        List<DowntimeEventDto> downtimes;
        try {
            downtimes = processingClient.getDowntimes(shiftId);
        } catch (Exception e) {
            log.warn("Could not fetch downtimes for shift {}: {}", shiftId, e.getMessage());
            downtimes = Collections.emptyList();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        addHeader(doc, k);
        addShiftMeta(doc, k);
        addKpiBlock(doc, k);
        addTimeBlock(doc, k);
        addOutputBlock(doc, k);
        addDowntimes(doc, downtimes);
        addFooter(doc);

        doc.close();
        return baos.toByteArray();
    }

    private void addHeader(Document doc, KpiResultDto k) {
        Paragraph title = new Paragraph("Отчёт по смене №" + k.getShiftId(), fonts.bold(18));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph sub = new Paragraph(
                "Линия: " + safe(k.getLineId()) + "  |  Сформировано: " + DTF.format(LocalDateTime.now()),
                fonts.italic(10));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        doc.add(sub);

        doc.add(Chunk.NEWLINE);
    }

    private void addShiftMeta(Document doc, KpiResultDto k) {
        section(doc, "Параметры смены");
        PdfPTable t = grid(2, new float[]{1.5f, 2f});
        kv(t, "ID смены", String.valueOf(k.getShiftId()));
        kv(t, "Линия", safe(k.getLineId()));
        kv(t, "Плановое время, мин", str(k.getPlannedTime()));
        kv(t, "Номинальная скорость, шт/мин", str(k.getNominalSpeed()));
        doc.add(t);
        doc.add(spacer());
    }

    private void addKpiBlock(Document doc, KpiResultDto k) {
        section(doc, "Ключевые показатели (OEE)");

        PdfPTable t = grid(4, new float[]{1, 1, 1, 1});
        t.setHeaderRows(1);
        addKpiHeader(t, "OEE");
        addKpiHeader(t, "Availability");
        addKpiHeader(t, "Performance");
        addKpiHeader(t, "Quality");

        addKpiValue(t, percent(k.getOee()), kpiColor(k.getOee(), 0.65));
        addKpiValue(t, percent(k.getAvailability()), kpiColor(k.getAvailability(), 0.80));
        addKpiValue(t, percent(k.getPerformance()), kpiColor(k.getPerformance(), 0.75));
        addKpiValue(t, percent(k.getQuality()), kpiColor(k.getQuality(), 0.95));
        doc.add(t);

        doc.add(spacer());

        PdfPTable derived = grid(2, new float[]{2.5f, 1.5f});
        kv(derived, "Performance Loss", percent(k.getPerformanceLoss()));
        kv(derived, "Plan Fulfillment", percent(k.getPlanFulfillment()));
        kv(derived, "Scrap Rate", percent(k.getScrapRate()));
        kv(derived, "Speed Loss, шт/мин", str(k.getSpeedLoss()));
        doc.add(derived);
        doc.add(spacer());
    }

    private void addTimeBlock(Document doc, KpiResultDto k) {
        section(doc, "Время");
        PdfPTable t = grid(2, new float[]{2.5f, 1.5f});
        kv(t, "Плановое время, мин", str(k.getPlannedTime()));
        kv(t, "Рабочее время, мин", str(k.getOperatingTime()));
        kv(t, "Суммарный простой, мин", str(k.getDowntime()));
        kv(t, "Доля простоя", percent(k.getDowntimeRate()));
        kv(t, "Количество остановок", str(k.getNumberOfStops()));
        kv(t, "Средн. длительность остановки, мин", str(k.getAvgDowntime()));
        doc.add(t);
        doc.add(spacer());
    }

    private void addOutputBlock(Document doc, KpiResultDto k) {
        section(doc, "Выпуск и скорость");
        PdfPTable t = grid(2, new float[]{2.5f, 1.5f});
        kv(t, "Всего выпущено, шт", str(k.getTotalOutput()));
        kv(t, "Годных, шт", str(k.getGoodOutput()));
        kv(t, "Брак, шт", str(k.getScrapCount()));
        kv(t, "План выпуска, шт", str(k.getPlannedOutput()));
        kv(t, "Средняя скорость, шт/мин", str(k.getAvgSpeed()));
        kv(t, "Output Rate, шт/мин", str(k.getOutputRate()));
        doc.add(t);
        doc.add(spacer());
    }

    private void addDowntimes(Document doc, List<DowntimeEventDto> events) {
        section(doc, "Журнал простоев (" + events.size() + ")");
        if (events.isEmpty()) {
            Paragraph p = new Paragraph("За смену простоев не зарегистрировано.", fonts.italic(10));
            p.setSpacingAfter(12);
            doc.add(p);
            return;
        }
        PdfPTable t = grid(4, new float[]{0.5f, 1.2f, 1.2f, 1f});
        t.setHeaderRows(1);
        Font th = fonts.bold(10);
        th.setColor(Color.WHITE);
        for (String h : new String[]{"#", "Начало", "Конец", "Длит., мин"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, th));
            c.setBackgroundColor(HEADER_BG);
            c.setPadding(6);
            t.addCell(c);
        }
        int i = 1;
        for (DowntimeEventDto d : events) {
            boolean alt = i % 2 == 0;
            row(t, String.valueOf(i++), alt);
            row(t, fmtDt(d.getStartTime()), alt);
            row(t, fmtDt(d.getEndTime()), alt);
            row(t, str(d.getDurationMinutes()), alt);
        }
        doc.add(t);
        doc.add(spacer());
    }

    private void addFooter(Document doc) {
        Font f = fonts.italic(8);
        f.setColor(Color.GRAY);
        Paragraph p = new Paragraph("Сформировано системой TeaPack KPI Monitoring", f);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }

    private void section(Document doc, String title) {
        Paragraph p = new Paragraph(title, fonts.bold(13));
        p.setSpacingBefore(6);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPTable grid(int cols, float[] widths) {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        try { t.setWidths(widths); } catch (Exception ignored) {}
        return t;
    }

    private void kv(PdfPTable t, String k, String v) {
        PdfPCell c1 = new PdfPCell(new Phrase(k, fonts.regular(10)));
        c1.setPadding(5);
        c1.setBackgroundColor(ROW_ALT);
        PdfPCell c2 = new PdfPCell(new Phrase(v == null ? "—" : v, fonts.bold(10)));
        c2.setPadding(5);
        t.addCell(c1);
        t.addCell(c2);
    }

    private void addKpiHeader(PdfPTable t, String text) {
        Font f = fonts.bold(11);
        f.setColor(Color.WHITE);
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(HEADER_BG);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(6);
        t.addCell(c);
    }

    private void addKpiValue(PdfPTable t, String value, Color color) {
        Font f = fonts.bold(16);
        f.setColor(color);
        PdfPCell c = new PdfPCell(new Phrase(value, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(10);
        t.addCell(c);
    }

    private void row(PdfPTable t, String text, boolean alt) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "—" : text, fonts.regular(10)));
        c.setPadding(5);
        if (alt) c.setBackgroundColor(ROW_ALT);
        t.addCell(c);
    }

    private Paragraph spacer() {
        Paragraph p = new Paragraph(" ", fonts.regular(6));
        p.setSpacingAfter(4);
        return p;
    }

    private String safe(String s) { return s == null ? "—" : s; }

    private String str(Object v) {
        if (v == null) return "—";
        if (v instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(v);
    }

    private String percent(BigDecimal v) {
        if (v == null) return "—";
        return v.movePointRight(2).setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String fmtDt(LocalDateTime dt) {
        return dt == null ? "—" : DTF.format(dt);
    }

    private Color kpiColor(BigDecimal v, double threshold) {
        if (v == null) return Color.DARK_GRAY;
        double d = v.doubleValue();
        if (d >= threshold) return new Color(0x52, 0xC4, 0x1A);
        if (d >= threshold * 0.8) return new Color(0xFA, 0xAD, 0x14);
        return new Color(0xFF, 0x4D, 0x4F);
    }
}
