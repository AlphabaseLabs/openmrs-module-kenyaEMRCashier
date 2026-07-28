package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.commons.lang.StringUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;

/**
 * Reusable page header handler that renders header on every page.
 * This component can be used across different document types.
 */
public class PageHeaderHandler {

    private static final String FACILITY_NAME_FALLBACK =
            "No facility name configured, please add facility name in the global property "
                    + "kenyaemr.cashier.receipt.facilityInformation";

    // Header positioning constants
    private static final float HEADER_TOP_MARGIN = 20f;
    private static final float HEADER_HEIGHT = 40f;
    private static final float SECTION_SPACING = 2f;

    private final HeaderConfig config;
    private String facilityName;

    /**
     * Constructor with default configuration
     */
    public PageHeaderHandler() {
        this(new HeaderConfig());
    }

    /**
     * Constructor with custom configuration
     * @param config Custom header configuration
     */
    public PageHeaderHandler(HeaderConfig config) {
        this.config = config != null ? config : new HeaderConfig();
    }

    /**
     * Renders the page header
     * @param canvas The PDF canvas to draw on
     * @param page The PDF page
     * @param data Document data for customization
     * @param pageNumber Current page number
     */
    public void renderHeader(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        String facilityName = getFacilityName();
        String documentNumber = PdfGenerationUtils.extractDocumentNumber(data);
        
        // Get page dimensions
        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();
        float pageHeight = pageSize.getHeight();
        
        // Calculate header position (top of page)
        float headerY = pageHeight - HEADER_TOP_MARGIN - HEADER_HEIGHT;
        float headerWidth = pageWidth - 100; // Leave margins
        float headerX = 50; // Left margin
        
        // Create header container with fixed position
        canvas.setFixedPosition(headerX, headerY, headerWidth);
        
        // Facility name
        canvas.add(new Paragraph(facilityName)
                .setFont(CarbonPdfFonts.semibold())
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(SECTION_SPACING));

        // Document number and page info
        if (StringUtils.isNotEmpty(config.documentType)) {
            canvas.add(new Paragraph()
                    .add(new Text(config.documentType + ": ").setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .add(new Text(documentNumber).setFont(CarbonPdfFonts.semibold()).setFontSize(8))
                    .add(new Text(" | Page ").setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .add(new Text(String.valueOf(pageNumber)).setFont(CarbonPdfFonts.semibold()).setFontSize(8))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        } else {
            canvas.add(new Paragraph()
                    .add(new Text("Document: ").setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .add(new Text(documentNumber).setFont(CarbonPdfFonts.semibold()).setFontSize(8))
                    .add(new Text(" | Page ").setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .add(new Text(String.valueOf(pageNumber)).setFont(CarbonPdfFonts.semibold()).setFontSize(8))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        }

        // Custom header text if provided
        if (StringUtils.isNotEmpty(config.customHeaderText)) {
            canvas.add(new Paragraph(config.customHeaderText)
                    .setFont(CarbonPdfFonts.regular())
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        }

        // Bottom separator line
        canvas.add(new Paragraph(" ")
                .setBorderBottom(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));
    }

    /**
     * Get facility name from global property or use default
     */
    private String getFacilityName() {
        if (facilityName == null) {
            facilityName = DocumentHeader.getConfiguredFacilityName(FACILITY_NAME_FALLBACK);
        }
        return facilityName;
    }

    /**
     * Header configuration class
     */
    public static class HeaderConfig {
        public String documentType = "";
        public String customHeaderText = "";

        public HeaderConfig() {}

        public HeaderConfig(String documentType, String customHeaderText) {
            this.documentType = documentType;
            this.customHeaderText = customHeaderText;
        }

        public HeaderConfig setDocumentType(String documentType) {
            this.documentType = documentType;
            return this;
        }

        public HeaderConfig setCustomHeaderText(String customHeaderText) {
            this.customHeaderText = customHeaderText;
            return this;
        }
    }
}
