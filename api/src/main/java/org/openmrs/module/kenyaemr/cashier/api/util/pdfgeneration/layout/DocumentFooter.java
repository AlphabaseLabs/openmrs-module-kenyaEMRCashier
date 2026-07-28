package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;

import java.util.Date;

/**
 * Reusable document footer component with customizable document description and
 * page handler support.
 * This component can be used across different document types.
 */
public class DocumentFooter {

    private static final String FACILITY_NAME_FALLBACK =
            "No facility name configured, please add facility name in the global property "
                    + "kenyaemr.cashier.receipt.facilityInformation";

    // Design constants
    private static final float FOOTER_TOP_MARGIN = 12f;
    private static final float SECTION_SPACING = 4f;
    private static final float LINE_SPACING = 1.5f;

    private final FooterConfig config;

    /**
     * Constructor with default configuration
     */
    public DocumentFooter() {
        this(new FooterConfig());
    }

    /**
     * Constructor with custom configuration
     * 
     * @param config Custom footer configuration
     */
    public DocumentFooter(FooterConfig config) {
        this.config = config != null ? config : new FooterConfig();
    }

    /**
     * Renders the document footer
     * 
     * @param doc  The PDF document to add the footer to
     * @param data Document data for customization
     */
    public void render(Document doc, Object data) {
        String facilityName = getFacilityName();

        // Top separator
        doc.add(new Paragraph(" ")
                .setMarginTop(FOOTER_TOP_MARGIN)
                .setBorderTop(new SolidBorder(0.5f))
                .setMarginBottom(SECTION_SPACING));

        // Document description
        if (StringUtils.isNotEmpty(config.documentDescription)) {
            doc.add(new Paragraph(config.documentDescription)
                    .setFont(CarbonPdfFonts.regular())
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Facility name and payment terms
        if (StringUtils.isNotEmpty(config.paymentTerms)) {
            doc.add(new Paragraph()
                    .add(new Text(facilityName).setFont(CarbonPdfFonts.semibold()).setFontSize(8))
                    .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .add(new Text(config.paymentTerms).setFont(CarbonPdfFonts.regular()).setFontSize(8))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(LINE_SPACING));
        }

        // Thank you message
        if (StringUtils.isNotEmpty(config.thankYouMessage)) {
            doc.add(new Paragraph()
                    .add(new Text("Thank you for choosing ").setFont(CarbonPdfFonts.regular()).setFontSize(7))
                    .add(new Text(facilityName).setFont(CarbonPdfFonts.semibold()).setFontSize(7))
                    .add(new Text(" ").setFont(CarbonPdfFonts.regular()).setFontSize(7))
                    .add(new Text(config.thankYouMessage).setFont(CarbonPdfFonts.regular()).setFontSize(7))
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(SECTION_SPACING));
        }

        // System note
        doc.add(createSystemNote(data));
    }

    /**
     * Renders the document footer with custom data
     * 
     * @param doc The PDF document to add the footer to
     */
    public void render(Document doc) {
        render(doc, null);
    }

    /**
     * Get facility name from global property or use default
     */
    private String getFacilityName() {
        return DocumentHeader.getConfiguredFacilityName(FACILITY_NAME_FALLBACK);
    }

    /**
     * Create system-generated note
     */
    private Paragraph createSystemNote(Object data) {
        String documentNumber = PdfGenerationUtils.extractDocumentNumber(data);
        String generatedDate = PdfGenerationUtils.formatSystemDate(new Date());
        String generatedBy = Context.getAuthenticatedUser() != null ? Context.getAuthenticatedUser().getUsername()
                : "system";

        Paragraph paragraph = new Paragraph();
        if (StringUtils.isNotEmpty(config.customFooterText)) {
            paragraph.add(new Text(normalizeFooterPrefix(config.customFooterText) + " | ")
                    .setFont(CarbonPdfFonts.regular()).setFontSize(6));
        }

        return paragraph
                .add(new Text("DOC NO: ").setFont(CarbonPdfFonts.regular()).setFontSize(6))
                .add(new Text(documentNumber).setFont(CarbonPdfFonts.semibold()).setFontSize(6))
                .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(6))
                .add(new Text(generatedDate).setFont(CarbonPdfFonts.regular()).setFontSize(6))
                .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(6))
                .add(new Text(generatedBy).setFont(CarbonPdfFonts.regular()).setFontSize(6))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(LINE_SPACING);
    }

    private String normalizeFooterPrefix(String customFooterText) {
        return customFooterText.trim().replaceAll("[\\s.,]+$", "");
    }

    /**
     * Footer configuration class
     */
    public static class FooterConfig {
        public String documentDescription = "";
        public String paymentTerms = "";
        public String thankYouMessage = "";
        public String customFooterText = "";

        public FooterConfig() {
        }

        public FooterConfig(String documentDescription, String paymentTerms, String thankYouMessage) {
            this.documentDescription = documentDescription;
            this.paymentTerms = paymentTerms;
            this.thankYouMessage = thankYouMessage;
        }

        public FooterConfig setDocumentDescription(String documentDescription) {
            this.documentDescription = documentDescription;
            return this;
        }

        public FooterConfig setPaymentTerms(String paymentTerms) {
            this.paymentTerms = paymentTerms;
            return this;
        }

        public FooterConfig setThankYouMessage(String thankYouMessage) {
            this.thankYouMessage = thankYouMessage;
            return this;
        }

        public FooterConfig setCustomFooterText(String customFooterText) {
            this.customFooterText = customFooterText;
            return this;
        }
    }
}
