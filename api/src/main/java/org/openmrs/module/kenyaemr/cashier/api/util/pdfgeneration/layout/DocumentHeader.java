package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.image.ImageDataFactory;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Reusable document header component containing logo, facility name, and
 * tagline.
 * This component can be used across different document types.
 * 
 * Usage examples:
 * - Basic header: new DocumentHeader().render(doc)
 * - With title: new DocumentHeader().setTitle("Bill Statement").render(doc)
 * - With title and subtitle: new DocumentHeader().setTitle("Bill
 * Statement").setSubtitle("Interim invoice").render(doc)
 * - With custom facility info: new
 * DocumentHeader().setFacilityInfo(customInfo).setTitle("Receipt").render(doc)
 */
public class DocumentHeader {

    private static final Logger log = LoggerFactory.getLogger(DocumentHeader.class);
    private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Design constants
    private static final float HEADER_SPACING = -8f;
    private static final float LOGO_MAX_WIDTH = 308f;
    private static final float LOGO_MAX_HEIGHT = 84f;
    private static final float FACILITY_NAME_FONT_SIZE = 11.5f;
    private static final float DETAIL_FONT_SIZE = 9.5f;

    // Instance variables for fluent API
    private FacilityInfo facilityInfo;
    private String documentTitle;
    private String documentSubtitle;

    /**
     * Set custom facility information
     * 
     * @param facilityInfo Custom facility information to use
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setFacilityInfo(FacilityInfo facilityInfo) {
        this.facilityInfo = facilityInfo;
        return this;
    }

    /**
     * Set the document title
     * 
     * @param title The title of the document (e.g., "Bill Statement")
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setTitle(String title) {
        this.documentTitle = title;
        return this;
    }

    /**
     * Set the document subtitle
     * 
     * @param subtitle The subtitle of the document (e.g., "This is an interim
     *                 invoice and may change")
     * @return this DocumentHeader instance for method chaining
     */
    public DocumentHeader setSubtitle(String subtitle) {
        this.documentSubtitle = subtitle;
        return this;
    }

    /**
     * Renders the document header with the configured settings
     * 
     * @param doc The PDF document to add the header to
     */
    public void render(Document doc) {
        // If no custom facility info is set, parse from global property
        if (facilityInfo == null) {
            facilityInfo = loadFacilityInformation();
        }

        createHeader(doc, facilityInfo, documentTitle, documentSubtitle);
    }

    /**
     * Parse facility information from global property with proper error handling
     */
    private static FacilityInfo loadFacilityInformation() {
        String facilityInfoJson = Context.getAdministrationService()
                .getGlobalProperty(GP_FACILITY_INFORMATION);

        FacilityInfo info = new FacilityInfo();

        if (StringUtils.isNotEmpty(facilityInfoJson)) {
            try {
                JsonNode facilityNode = OBJECT_MAPPER.readTree(facilityInfoJson);
                info.facilityName = getJsonValue(facilityNode, "facilityName", info.facilityName);
                info.tagline = getJsonValue(facilityNode, "tagline", info.tagline);
                info.logoPath = getJsonValue(facilityNode, "logoPath", info.logoPath);
                info.logoData = getJsonValue(facilityNode, "logoData", info.logoData);
                String topLevelTel = getJsonValue(facilityNode, "tel", "");
                // Parse contacts if present
                if (facilityNode.has("contacts")) {
                    JsonNode contactsNode = facilityNode.get("contacts");
                    info.contacts = new FacilityContacts();
                    info.contacts.tel = getJsonValue(contactsNode, "tel", topLevelTel);
                    info.contacts.email = getJsonValue(contactsNode, "email", "");
                    info.contacts.address = getJsonValue(contactsNode, "address", "");
                    info.contacts.web = getJsonValue(contactsNode, "website", "");
                    info.contacts.emergency = getJsonValue(contactsNode, "emergency", "");
                } else if (StringUtils.isNotEmpty(topLevelTel)) {
                    info.contacts = new FacilityContacts();
                    info.contacts.tel = topLevelTel;
                }
            } catch (Exception e) {
                log.warn("Failed to parse facility information JSON. Using defaults.", e);
            }
        }

        return info;
    }

    static String getConfiguredFacilityName(String fallback) {
        FacilityInfo info = getConfiguredFacilityInfo();
        return StringUtils.isNotEmpty(info.facilityName) ? info.facilityName : fallback;
    }

    static FacilityInfo getConfiguredFacilityInfo() {
        return loadFacilityInformation();
    }

    private static String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asText() : defaultValue;
    }

    /**
     * Create header layout with logo, facility name/tagline, and optional document
     * title/subtitle
     */
    private void createHeader(Document doc, FacilityInfo info, String documentTitle, String documentSubtitle) {
        // Ensure facility information is always included
        if (info == null || (StringUtils.isEmpty(info.facilityName) && StringUtils.isEmpty(info.tagline))) {
            info = loadFacilityInformation();
        }

        String facilityNameText = StringUtils.isNotEmpty(info.facilityName) ? info.facilityName
                : "Facility Name Not Configured";
        Paragraph facilityName = new Paragraph(facilityNameText)
                .setFont(CarbonPdfFonts.semibold())
                .setFontSize(FACILITY_NAME_FONT_SIZE)
                .setFontColor(PrintablePdfStyle.GREEN)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(2f)
                .setMarginTop(0f)
                .setMarginRight(0)
                .setMarginLeft(0);

        Div textBlock = new Div()
                .add(facilityName)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(0)
                .setMargin(0)
                .setVerticalAlignment(VerticalAlignment.TOP);

        if (StringUtils.isNotEmpty(info.tagline)) {
            textBlock.add(createDetailLine(info.tagline));
        }

        Image logo = createCenteredLogo(info);

        float[] columnWidths = { 50f, 50f };
        Table headerTable = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMargin(0)
                .setPadding(0);

        if (logo != null) {
            constrainLogo(logo);
            headerTable.addCell(new Cell()
                    .add(logo)
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setPaddingTop(0)
                    .setPaddingRight(8f)
                    .setPaddingBottom(0)
                    .setPaddingLeft(0)
                    .setMargin(0));
        } else {
            headerTable.addCell(new Cell()
                    .add(new Paragraph(facilityNameText).setFont(CarbonPdfFonts.regular()).setFontSize(14f)
                            .setFontColor(PrintablePdfStyle.TEXT).setTextAlignment(TextAlignment.LEFT).setMargin(0))
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setPaddingTop(0)
                    .setPaddingRight(8f)
                    .setPaddingBottom(0)
                    .setPaddingLeft(0)
                    .setMargin(0));
        }

        headerTable.addCell(new Cell()
                .add(textBlock)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPaddingTop(0)
                .setPaddingRight(0)
                .setPaddingBottom(0)
                .setPaddingLeft(10f)
                .setMargin(0));

        doc.add(headerTable);

        if (StringUtils.isNotEmpty(documentTitle)) {
            Paragraph title = new Paragraph(documentTitle.toUpperCase())
                    .setFont(CarbonPdfFonts.regular())
                    .setFontSize(30f)
                    .setFontColor(PrintablePdfStyle.TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(HEADER_SPACING)
                    .setMarginBottom(StringUtils.isNotEmpty(documentSubtitle) ? 2f : 8f);
            doc.add(title);
        }

        if (StringUtils.isNotEmpty(documentSubtitle)) {
            doc.add(new Paragraph(documentSubtitle)
                    .setFont(CarbonPdfFonts.regular())
                    .setFontSize(10f)
                    .setFontColor(PrintablePdfStyle.SECONDARY_TEXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0)
                    .setMarginBottom(12f));
        }
    }

    private void constrainLogo(Image logo) {
        logo.setAutoScale(false);
        float width = logo.getImageWidth();
        float height = logo.getImageHeight();
        if (width > 0 && height > 0) {
            float scale = Math.min(LOGO_MAX_WIDTH / width, LOGO_MAX_HEIGHT / height);
            if (scale < 1f) {
                logo.scale(scale, scale);
            }
        }
        logo.setMaxWidth(LOGO_MAX_WIDTH);
        logo.setMaxHeight(LOGO_MAX_HEIGHT);
        logo.setMargins(0, 0, 0, 0);
        logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);
    }

    private Paragraph createDetailLine(String value) {
        return new Paragraph()
                .add(new Text(value).setFont(CarbonPdfFonts.regular()).setFontSize(DETAIL_FONT_SIZE)
                        .setFontColor(PrintablePdfStyle.TEXT))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFixedLeading(12f)
                .setMarginTop(0)
                .setMarginBottom(2f)
                .setMarginLeft(0)
                .setMarginRight(0);
    }

    /**
     * Create a centered logo image (returns null if not found)
     */
    private Image createCenteredLogo(FacilityInfo info) {
        try {
            byte[] imageBytes = null;
            // First try to use logo data from global property (base64 encoded)
            if (StringUtils.isNotEmpty(info.logoData)) {
                try {
                    imageBytes = java.util.Base64.getDecoder().decode(info.logoData);
                } catch (Exception e) {
                    log.warn("Failed to decode base64 logo data", e);
                }
            }
            // If no logo data, try to use logo path from global property
            if (imageBytes == null && StringUtils.isNotEmpty(info.logoPath)) {
                try {
                    java.io.File logoFile = new java.io.File(info.logoPath);
                    if (logoFile.exists()) {
                        imageBytes = java.nio.file.Files.readAllBytes(logoFile.toPath());
                    } else {
                        InputStream inputStream = getClass().getResourceAsStream(info.logoPath);
                        if (inputStream != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) != -1) {
                                baos.write(buffer, 0, length);
                            }
                            imageBytes = baos.toByteArray();
                            inputStream.close();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to load logo from path: " + info.logoPath, e);
                }
            }

            if (imageBytes != null) {
                return new Image(ImageDataFactory.create(imageBytes));
            }
        } catch (Exception e) {
            log.warn("Failed to create logo image", e);
        }

        return null;
    }

    /**
     * Facility information data class
     */
    public static class FacilityInfo {
        public String facilityName = "";
        public String tagline = "";
        public String logoPath = "";
        public String logoData = "";
        public FacilityContacts contacts = null;

        public FacilityInfo() {
        }

        public FacilityInfo(String facilityName, String tagline, String logoPath, String logoData) {
            this.facilityName = facilityName;
            this.tagline = tagline;
            this.logoPath = logoPath;
            this.logoData = logoData;
        }
    }

    public static class FacilityContacts {
        public String tel = "";
        public String email = "";
        public String address = "";
        public String web = "";
        public String emergency = "";

        public boolean hasAny() {
            return StringUtils.isNotEmpty(tel) || StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(address)
                    || StringUtils.isNotEmpty(web) || StringUtils.isNotEmpty(emergency);
        }
    }
}
