package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Canvas;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PageFooterHandler;

public class BillStatementPageFooterHandler implements PdfDocumentService.PageFooterHandler {

    private final PageFooterHandler delegate;

    public BillStatementPageFooterHandler() {
        this.delegate = new PageFooterHandler(new PageFooterHandler.FooterConfig(
                "This bill statement is computer-generated and valid without signature.", "", ""));
    }

    @Override
    public void renderFooter(Canvas canvas, PdfPage page, Object data, int pageNumber) {
        delegate.renderFooter(canvas, page, data, pageNumber);
    }
}
