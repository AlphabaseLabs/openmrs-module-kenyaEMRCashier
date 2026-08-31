package org.openmrs.module.kenyaemr.cashier.api.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BillServiceImplReceiptNoteTest {

    @Test
    public void createReceiptNote_shouldRenderTrimmedMultilineNote() throws IOException {
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont semibold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        final Paragraph note = BillServiceImpl.createReceiptNote(
                "  Receipt note line one.\nReceipt note line two.  ", regular, semibold);

        File pdf = new PdfDocumentServiceImpl().generatePdf("receipt-note-test", null, null,
                new PdfDocumentService.ContentSection() {
                    @Override
                    public void render(Document doc, Object data) {
                        doc.add(note);
                    }
                }, null);

        try {
            String text = extractText(pdf);
            assertTrue(text.contains("Note: Receipt note line one."));
            assertTrue(text.contains("Receipt note line two."));
        }
        finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    @Test
    public void createReceiptNote_shouldOmitBlankNote() throws IOException {
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont semibold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        assertNull(BillServiceImpl.createReceiptNote("  \n  ", regular, semibold));
    }

    private String extractText(File pdf) throws IOException {
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdf));
        try {
            return PdfTextExtractor.getTextFromPage(pdfDocument.getFirstPage());
        }
        finally {
            pdfDocument.close();
        }
    }
}
