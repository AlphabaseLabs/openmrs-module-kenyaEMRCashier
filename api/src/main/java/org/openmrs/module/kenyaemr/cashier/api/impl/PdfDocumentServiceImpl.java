package org.openmrs.module.kenyaemr.cashier.api.impl;

import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.CarbonPdfFonts;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PaidStampRenderer;

import java.io.File;
import java.io.IOException;

public class PdfDocumentServiceImpl implements PdfDocumentService {

    private static final float LEFT_MARGIN = 24f;
    private static final float RIGHT_MARGIN = 24f;
    private static final float TOP_MARGIN = 10f;
    private static final float BOTTOM_MARGIN = 56f;

    @Override
    public File generatePdf(String documentType, Object data, 
                            LetterheadSection letterhead, ContentSection content, FooterSection footer) {
        return generatePdf(documentType, data, letterhead, content, footer, null, null);
    }
    
    /**
     * Enhanced method that supports page-level headers and footers
     */
    public File generatePdf(String documentType, Object data, 
                            LetterheadSection letterhead, ContentSection content, FooterSection footer,
                            PageHeaderHandler pageHeader, PageFooterHandler pageFooter) {
        File tempFile = null;
        try (CarbonPdfFonts.FontScope ignored = CarbonPdfFonts.openDocumentScope()) {
            tempFile = File.createTempFile(documentType + "_", ".pdf");
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(tempFile));

            try {
                boolean renderPaidStamp = PaidStampRenderer.shouldRender(data);
                if (pageHeader != null || pageFooter != null || renderPaidStamp) {
                    pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE,
                            new PageEventHandler(data, pageHeader, pageFooter, renderPaidStamp));
                }

                Document doc = new Document(pdfDoc, PageSize.A4);
                doc.setMargins(TOP_MARGIN, RIGHT_MARGIN, BOTTOM_MARGIN, LEFT_MARGIN);

                if (letterhead != null) {
                    letterhead.render(doc, data);
                }
                if (content != null) {
                    content.render(doc, data);
                }
                if (footer != null) {
                    footer.render(doc, data);
                }

                doc.close();
            } finally {
                if (!pdfDoc.isClosed()) {
                    pdfDoc.close();
                }
            }
            return tempFile;
        } catch (IOException e) {
            deleteTempFile(tempFile);
            throw new RuntimeException("Failed to generate PDF", e);
        } catch (RuntimeException e) {
            deleteTempFile(tempFile);
            throw e;
        }
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
            tempFile.deleteOnExit();
        }
    }

    private static final class PageEventHandler implements IEventHandler {

        private final Object data;
        private final PageHeaderHandler pageHeaderHandler;
        private final PageFooterHandler pageFooterHandler;
        private final boolean renderPaidStamp;

        private PageEventHandler(Object data, PageHeaderHandler pageHeaderHandler,
                PageFooterHandler pageFooterHandler, boolean renderPaidStamp) {
            this.data = data;
            this.pageHeaderHandler = pageHeaderHandler;
            this.pageFooterHandler = pageFooterHandler;
            this.renderPaidStamp = renderPaidStamp;
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            PdfDocument pdfDoc = docEvent.getDocument();
            int pageNumber = pdfDoc.getPageNumber(page);
            
            Rectangle pageSize = page.getPageSize();
            float pageWidth = pageSize.getWidth();
            PdfCanvas canvas = new PdfCanvas(page);

            if (pageNumber == 1 && renderPaidStamp) {
                PaidStampRenderer.render(canvas, pageSize);
            }

            if (pageHeaderHandler != null) {
                Rectangle headerRect = new Rectangle(
                    pageSize.getLeft() + LEFT_MARGIN,
                    pageSize.getTop() - TOP_MARGIN,
                    pageWidth - LEFT_MARGIN - RIGHT_MARGIN,
                    TOP_MARGIN
                );
                Canvas headerCanvas = new Canvas(canvas, headerRect);
                pageHeaderHandler.renderHeader(headerCanvas, page, data, pageNumber);
                headerCanvas.close();
            }

            if (pageFooterHandler != null) {
                Rectangle footerRect = new Rectangle(
                    pageSize.getLeft() + LEFT_MARGIN,
                    pageSize.getBottom(),
                    pageWidth - LEFT_MARGIN - RIGHT_MARGIN,
                    BOTTOM_MARGIN
                );
                Canvas footerCanvas = new Canvas(canvas, footerRect);
                pageFooterHandler.renderFooter(footerCanvas, page, data, pageNumber);
                footerCanvas.close();
            }

            canvas.release();
        }
    }
}
