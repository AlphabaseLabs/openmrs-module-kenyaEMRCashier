package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PageFooterHandler;

public class InvoicePageFooterHandler implements PdfDocumentService.PageFooterHandler {

    private final PageFooterHandler pageFooterHandler;

    public InvoicePageFooterHandler() {
        this.pageFooterHandler = new PageFooterHandler(new PageFooterHandler.FooterConfig(
                "This invoice is computer-generated and valid without signature.", "", ""));
    }

    @Override
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        pageFooterHandler.renderFooter(canvas, page, data, pageNumber);
    }
}
