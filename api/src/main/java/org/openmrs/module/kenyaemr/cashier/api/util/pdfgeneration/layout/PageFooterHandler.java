package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.apache.commons.lang.StringUtils;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;

import java.util.Date;

/**
 * Reusable page footer handler that renders footer on every page.
 * This component can be used across different document types.
 */
public class PageFooterHandler {

    private static final String FACILITY_NAME_FALLBACK =
            "No facility name configured, please add facility name in the global property "
                    + "kenyaemr.cashier.receipt.facilityInformation";
    private static final float FOOTER_BOTTOM_MARGIN = 16f;
    private static final float FOOTER_LINE_SPACING = 2f;

    private final FooterConfig config;
    private final Date generatedAt;
    private DocumentHeader.FacilityInfo facilityInfo;
    private String facilityName;

    /**
     * Constructor with default configuration
     */
    public PageFooterHandler() {
        this(new FooterConfig());
    }

    /**
     * Constructor with custom configuration
     * @param config Custom footer configuration
     */
    public PageFooterHandler(FooterConfig config) {
        this.config = config != null ? config : new FooterConfig();
        this.generatedAt = new Date();
    }

    /**
     * Renders the page footer
     * @param canvas The PDF canvas to draw on
     * @param page The PDF page
     * @param data Document data for customization
     * @param pageNumber Current page number
     */
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        String facilityName = getFacilityName();
        String facilityTel = getFacilityTel();
        String documentNumber = PdfGenerationUtils.extractDocumentNumber(data);

        Rectangle pageSize = page.getPageSize();
        float pageWidth = pageSize.getWidth();
        float footerX = 24f;
        float footerWidth = pageWidth - 48f;

        canvas.setFixedPosition(footerX, FOOTER_BOTTOM_MARGIN, footerWidth);
        canvas.add(new Paragraph(" ")
                .setBorderTop(new SolidBorder(PrintablePdfStyle.TEXT, 0.5f))
                .setMarginTop(0)
                .setMarginBottom(5f));

        if (StringUtils.isNotEmpty(config.paymentTerms)) {
            canvas.add(new Paragraph()
                    .add(new Text(facilityName).setFont(CarbonPdfFonts.semibold()).setFontSize(6.5f)
                            .setFontColor(PrintablePdfStyle.TEXT))
                    .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                            .setFontColor(PrintablePdfStyle.TEXT))
                    .add(new Text(config.paymentTerms).setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                            .setFontColor(PrintablePdfStyle.TEXT))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(0)
                    .setMarginBottom(FOOTER_LINE_SPACING));
        }

        String thankYouMessageTemplate = getThankYouMessageTemplate();
        if (StringUtils.isNotEmpty(thankYouMessageTemplate)) {
            Paragraph thankYouMessage = new Paragraph()
                    .setFontSize(7.5f)
                    .setFontColor(PrintablePdfStyle.TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0)
                    .setMarginBottom(FOOTER_LINE_SPACING);
            addFormattedThankYouMessage(thankYouMessage, thankYouMessageTemplate, facilityName, facilityTel);
            canvas.add(thankYouMessage);
        }

        canvas.add(createSystemNote(documentNumber, pageNumber, config.customFooterText));
    }

    private Paragraph createSystemNote(String documentNumber, int pageNumber, String customFooterText) {
        String generatedDateTime = PdfGenerationUtils.formatSystemTimestamp(generatedAt);
        User authenticatedUser = Context.getAuthenticatedUser();
        String generatedBy = authenticatedUser != null && authenticatedUser.getUsername() != null
                ? authenticatedUser.getUsername()
                : "system";
        String generatedByUserId = authenticatedUser != null && authenticatedUser.getId() != null
                ? authenticatedUser.getId().toString()
                : "N/A";

        Paragraph paragraph = new Paragraph();
        if (StringUtils.isNotEmpty(customFooterText)) {
            paragraph.add(new Text(normalizeFooterPrefix(customFooterText) + " | ").setFont(CarbonPdfFonts.regular())
                    .setFontSize(6.5f).setFontColor(PrintablePdfStyle.TEXT));
        }

        return paragraph
                .add(new Text("DOC NO: ").setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(documentNumber).setFont(CarbonPdfFonts.semibold()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(generatedDateTime).setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(" | ").setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(generatedBy + " (" + generatedByUserId + ")").setFont(CarbonPdfFonts.regular())
                        .setFontSize(6.5f).setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(" | Page ").setFont(CarbonPdfFonts.regular()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .add(new Text(String.valueOf(pageNumber)).setFont(CarbonPdfFonts.semibold()).setFontSize(6.5f)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(0)
                .setMarginBottom(0);
    }

    private String normalizeFooterPrefix(String customFooterText) {
        return customFooterText.trim().replaceAll("[\\s.,]+$", "");
    }

    private String getFacilityName() {
        if (facilityName == null) {
            DocumentHeader.FacilityInfo info = getFacilityInfo();
            facilityName = StringUtils.isNotEmpty(info.facilityName) ? info.facilityName : FACILITY_NAME_FALLBACK;
        }
        return facilityName;
    }

    private String getFacilityTel() {
        DocumentHeader.FacilityInfo info = getFacilityInfo();
        return info.contacts != null && StringUtils.isNotEmpty(info.contacts.tel) ? info.contacts.tel : "";
    }

    private DocumentHeader.FacilityInfo getFacilityInfo() {
        if (facilityInfo == null) {
            facilityInfo = DocumentHeader.getConfiguredFacilityInfo();
        }
        return facilityInfo;
    }

    private String getThankYouMessageTemplate() {
        return StringUtils.isNotEmpty(config.thankYouMessage)
                ? config.thankYouMessage
                : ModuleSettings.getPdfFooterThankYouMessage();
    }

    static String formatThankYouMessage(String template, String facilityName, String facilityTel) {
        if (StringUtils.isEmpty(template)) {
            return "";
        }

        return template
                .replace("{facilityName}", StringUtils.defaultString(facilityName))
                .replace("{facilityTel}", StringUtils.defaultString(facilityTel));
    }

    static void addFormattedThankYouMessage(Paragraph paragraph, String template, String facilityName,
            String facilityTel) {
        int index = 0;
        while (index < template.length()) {
            int facilityNameIndex = template.indexOf("{facilityName}", index);
            int facilityTelIndex = template.indexOf("{facilityTel}", index);
            int nextIndex = nextPlaceholderIndex(facilityNameIndex, facilityTelIndex);

            if (nextIndex < 0) {
                paragraph.add(footerText(template.substring(index), false));
                return;
            }

            if (nextIndex > index) {
                paragraph.add(footerText(template.substring(index, nextIndex), false));
            }

            if (nextIndex == facilityNameIndex) {
                paragraph.add(footerText(StringUtils.defaultString(facilityName), true));
                index = facilityNameIndex + "{facilityName}".length();
            } else {
                paragraph.add(footerText(StringUtils.defaultString(facilityTel), true));
                index = facilityTelIndex + "{facilityTel}".length();
            }
        }
    }

    private static int nextPlaceholderIndex(int facilityNameIndex, int facilityTelIndex) {
        if (facilityNameIndex < 0) {
            return facilityTelIndex;
        }
        if (facilityTelIndex < 0) {
            return facilityNameIndex;
        }
        return Math.min(facilityNameIndex, facilityTelIndex);
    }

    private static Text footerText(String text, boolean emphasized) {
        return new Text(text)
                .setFont(emphasized ? CarbonPdfFonts.semibold() : CarbonPdfFonts.regular())
                .setFontSize(7.5f)
                .setFontColor(PrintablePdfStyle.TEXT);
    }

    /**
     * Footer configuration class
     */
    public static class FooterConfig {
        public String customFooterText = "";
        public String paymentTerms = "";
        public String thankYouMessage = "";

        public FooterConfig() {}

        public FooterConfig(String customFooterText, String paymentTerms, String thankYouMessage) {
            this.customFooterText = customFooterText;
            this.paymentTerms = paymentTerms;
            this.thankYouMessage = thankYouMessage;
        }

        public FooterConfig setCustomFooterText(String customFooterText) {
            this.customFooterText = customFooterText;
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
    }
}
