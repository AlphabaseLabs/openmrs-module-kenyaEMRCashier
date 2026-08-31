package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

public final class PrintablePdfStyle {

    public static final DeviceRgb TEXT = new DeviceRgb(22, 22, 22);
    public static final DeviceRgb SECONDARY_TEXT = new DeviceRgb(82, 82, 82);
    public static final DeviceRgb GREEN = new DeviceRgb(14, 96, 39);
    public static final DeviceRgb TABLE_HEADER = new DeviceRgb(224, 224, 224);
    public static final DeviceRgb TABLE_ROW = new DeviceRgb(244, 244, 244);
    public static final DeviceRgb BORDER = new DeviceRgb(198, 198, 198);

    private static final float COMPACT_SUMMARY_WIDTH = 44f;

    private PrintablePdfStyle() {
    }

    public static Paragraph sectionHeading(String text) {
        return new Paragraph(text)
                .setFont(CarbonPdfFonts.semibold())
                .setFontSize(12f)
                .setFontColor(TEXT)
                .setMarginTop(0)
                .setMarginBottom(10f);
    }

    public static Table infoLine(String label, String value) {
        return infoLine(label, value, false, 0);
    }

    public static Table emphasizedInfoLine(String label, String value, float topGap) {
        return infoLine(label, value, true, topGap);
    }

    public static Paragraph inlineInfoLine(String label, String value) {
        return inlineInfoLine(label, value, false);
    }

    public static Paragraph inlineInfoLine(String label, String value, boolean emphasizedValue) {
        return new Paragraph()
                .add(new Text(label + ": ").setFont(CarbonPdfFonts.regular()))
                .add(new Text(safe(value))
                        .setFont(emphasizedValue ? CarbonPdfFonts.semibold() : CarbonPdfFonts.regular()))
                .setFontSize(10.5f)
                .setFixedLeading(13.5f)
                .setFontColor(TEXT)
                .setMargin(0);
    }

    private static Table infoLine(String label, String value, boolean emphasized, float topGap) {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 46f, 54f }))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(topGap)
                .setMarginBottom(0);

        table.addCell(infoCell(label + ":", TextAlignment.LEFT, emphasized));
        table.addCell(infoCell(safe(value), TextAlignment.RIGHT, emphasized));

        return table;
    }

    private static Cell infoCell(String text, TextAlignment alignment, boolean emphasized) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(emphasized ? CarbonPdfFonts.semibold() : CarbonPdfFonts.regular())
                        .setFontSize(10.5f)
                        .setFixedLeading(13.5f)
                        .setFontColor(TEXT)
                        .setMargin(0))
                .setTextAlignment(alignment)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
    }

    public static Cell detailCell() {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(3f)
                .setVerticalAlignment(VerticalAlignment.TOP);
    }

    public static Cell tableHeaderCell(String text) {
        return tableHeaderCell(text, TextAlignment.LEFT);
    }

    public static Cell tableHeaderCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFont(CarbonPdfFonts.semibold()).setFontSize(10.5f)
                        .setFontColor(SECONDARY_TEXT).setMargin(0))
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(TABLE_HEADER)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(4.5f)
                .setPaddingRight(6f)
                .setPaddingBottom(4.5f)
                .setPaddingLeft(6f);
    }

    public static Cell tableCell(String text) {
        return tableCell(text, TextAlignment.LEFT);
    }

    public static Cell tableCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text)).setFont(CarbonPdfFonts.regular()).setFontSize(10.5f)
                        .setFontColor(SECONDARY_TEXT).setMargin(0))
                .setTextAlignment(alignment)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(TABLE_ROW)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(4.5f)
                .setPaddingRight(6f)
                .setPaddingBottom(4.5f)
                .setPaddingLeft(6f);
    }

    public static Cell summaryLabelCell(String text) {
        return summaryCell(text, TextAlignment.LEFT);
    }

    public static Cell summaryValueCell(String text) {
        return summaryCell(text, TextAlignment.RIGHT);
    }

    public static Table compactSummaryTable(float bottomMargin) {
        return new Table(UnitValue.createPercentArray(new float[] { 44f, 56f }))
                .setWidth(UnitValue.createPercentValue(COMPACT_SUMMARY_WIDTH))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginTop(0)
                .setMarginBottom(bottomMargin)
                .setKeepTogether(true);
    }

    public static void addCompactSummaryRow(Table table, String label, String value, boolean emphasized) {
        addCompactSummaryRow(table, label, value, emphasized, 0);
    }

    public static void addCompactSummaryRow(Table table, String label, String value, boolean emphasized, float topGap) {
        table.addCell(compactSummaryCell(label, TextAlignment.LEFT, emphasized, topGap));
        table.addCell(compactSummaryCell(value, TextAlignment.RIGHT, emphasized, topGap));
    }

    public static Paragraph emptyState(String text, float bottomMargin) {
        return new Paragraph(safe(text))
                .setFont(CarbonPdfFonts.regular())
                .setItalic()
                .setFontSize(10f)
                .setFontColor(SECONDARY_TEXT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(bottomMargin);
    }

    public static Paragraph noteBlock(String text) {
        return new Paragraph()
                .add(new Text("Note:\n")
                        .setFont(CarbonPdfFonts.semibold())
                        .setFontColor(TEXT))
                .add(new Text(safe(text))
                        .setFont(CarbonPdfFonts.regular())
                        .setFontColor(SECONDARY_TEXT))
                .setFontSize(10.5f)
                .setFixedLeading(13.5f)
                .setMarginTop(10f)
                .setMarginBottom(0);
    }

    private static Cell summaryCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text)).setFont(CarbonPdfFonts.semibold()).setFontSize(12f)
                        .setFontColor(TEXT).setMargin(0))
                .setTextAlignment(alignment)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(BORDER, 0.5f))
                .setBorderBottom(new SolidBorder(BORDER, 0.5f))
                .setPaddingTop(5f)
                .setPaddingRight(4f)
                .setPaddingBottom(5f)
                .setPaddingLeft(4f);
    }

    private static Cell compactSummaryCell(String text, TextAlignment alignment, boolean emphasized) {
        return compactSummaryCell(text, alignment, emphasized, 0);
    }

    private static Cell compactSummaryCell(String text, TextAlignment alignment, boolean emphasized, float topGap) {
        return new Cell()
                .add(new Paragraph(safe(text))
                        .setFont(emphasized ? CarbonPdfFonts.semibold() : CarbonPdfFonts.regular())
                        .setFontSize(emphasized ? 11.5f : 10.5f)
                        .setFixedLeading(emphasized ? 14f : 12.5f)
                        .setFontColor(TEXT)
                        .setMargin(0))
                .setTextAlignment(alignment)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(topGap + (emphasized ? 4f : 1f))
                .setPaddingRight(4f)
                .setPaddingBottom(1f)
                .setPaddingLeft(4f);
    }

    private static String safe(String text) {
        return text != null ? text : "";
    }
}
