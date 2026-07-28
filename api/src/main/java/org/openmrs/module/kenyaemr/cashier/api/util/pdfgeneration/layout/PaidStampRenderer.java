package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;

import java.math.BigDecimal;

public final class PaidStampRenderer {

    private static final String STAMP_TEXT = "PAID";
    private static final DeviceRgb STAMP_GREEN = new DeviceRgb(0, 168, 84);
    private static final float OUTER_RADIUS = 39f;
    private static final float INNER_RADIUS = 30f;
    private static final float STAMP_OPACITY = 0.48f;
    private static final float TEXT_SIZE = 22f;
    private static final double TEXT_ANGLE = Math.toRadians(14);
    private static final float CENTER_X_FROM_RIGHT = 285f;
    private static final float CENTER_Y_FROM_BOTTOM = 305f;
    private static final boolean DEFAULT_PAID_STAMP_ENABLED = true;

    private PaidStampRenderer() {
    }

    public static boolean shouldRender(Object data) {
        return isPaidStampEnabled() && isFullyPaidBill(data);
    }

    static boolean shouldRender(Object data, boolean paidStampEnabled) {
        return paidStampEnabled && isFullyPaidBill(data);
    }

    static boolean isPaidStampEnabled(String configuredValue) {
        if (configuredValue == null || configuredValue.trim().isEmpty()) {
            return DEFAULT_PAID_STAMP_ENABLED;
        }
        return Boolean.parseBoolean(configuredValue.trim());
    }

    private static boolean isPaidStampEnabled() {
        try {
            String configuredValue = Context.getAdministrationService()
                    .getGlobalProperty(ModuleSettings.PDF_PAID_STAMP_ENABLED_PROPERTY);
            return isPaidStampEnabled(configuredValue);
        } catch (RuntimeException e) {
            return DEFAULT_PAID_STAMP_ENABLED;
        }
    }

    private static boolean isFullyPaidBill(Object data) {
        if (!(data instanceof Bill)) {
            return false;
        }

        Bill bill = (Bill) data;
        return BillStatus.PAID.equals(bill.getStatus())
                && bill.getBalance() != null
                && bill.getBalance().compareTo(BigDecimal.ZERO) == 0;
    }

    public static void render(PdfCanvas canvas, Rectangle pageSize) {
        float centerX = pageSize.getRight() - CENTER_X_FROM_RIGHT;
        float centerY = pageSize.getBottom() + CENTER_Y_FROM_BOTTOM;

        canvas.saveState();
        canvas.setExtGState(new PdfExtGState()
                .setStrokeOpacity(STAMP_OPACITY)
                .setFillOpacity(STAMP_OPACITY));
        canvas.setStrokeColor(STAMP_GREEN);
        canvas.setFillColor(STAMP_GREEN);
        canvas.setLineWidth(3f);
        canvas.circle(centerX, centerY, OUTER_RADIUS);
        canvas.stroke();
        canvas.setLineWidth(1.5f);
        canvas.circle(centerX, centerY, INNER_RADIUS);
        canvas.stroke();
        drawCenteredRotatedText(canvas, centerX, centerY);
        canvas.restoreState();
    }

    private static void drawCenteredRotatedText(PdfCanvas canvas, float centerX, float centerY) {
        float textWidth = CarbonPdfFonts.semibold().getWidth(STAMP_TEXT, TEXT_SIZE);
        float localX = -textWidth / 2f;
        float localY = -TEXT_SIZE * 0.33f;
        float cos = (float) Math.cos(TEXT_ANGLE);
        float sin = (float) Math.sin(TEXT_ANGLE);
        float textX = centerX + (localX * cos) - (localY * sin);
        float textY = centerY + (localX * sin) + (localY * cos);

        canvas.beginText()
                .setFontAndSize(CarbonPdfFonts.semibold(), TEXT_SIZE)
                .setTextMatrix(cos, sin, -sin, cos, textX, textY)
                .showText(STAMP_TEXT)
                .endText();
    }
}
