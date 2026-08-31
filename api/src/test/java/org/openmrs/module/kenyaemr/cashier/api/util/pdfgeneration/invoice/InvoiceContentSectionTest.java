package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvoiceContentSectionTest {

    @Test
    public void render_shouldAppendBillNoteAfterInvoiceSummary() throws IOException {
        Bill bill = billWithNote("Patient requested an itemized invoice.\nPlease retain this copy.");

        File pdf = generateInvoice(bill);

        try {
            String text = extractText(pdf);
            assertTrue(text.contains("Note:"));
            assertTrue(text.contains("Patient requested an itemized invoice."));
            assertTrue(text.contains("Please retain this copy."));
            assertTrue(text.indexOf("Balance:") < text.indexOf("Note:"));
        }
        finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    @Test
    public void render_shouldOmitBlankBillNote() throws IOException {
        File pdf = generateInvoice(billWithNote("  \n  "));

        try {
            assertFalse(extractText(pdf).contains("Note:"));
        }
        finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    private File generateInvoice(Bill bill) {
        return new PdfDocumentServiceImpl().generatePdf("invoice-note-test", bill, null,
                new InvoiceContentSection(), null);
    }

    private Bill billWithNote(String note) {
        BillLineItem lineItem = new BillLineItem();
        lineItem.setPrice(new BigDecimal("100.00"));
        lineItem.setQuantity(1);
        lineItem.setVoided(false);

        Bill bill = new Bill();
        bill.setLineItems(Arrays.asList(lineItem));
        bill.setPayments(new HashSet<>());
        bill.setNote(note);
        return bill;
    }

    private String extractText(File pdf) throws IOException {
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdf));
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= pdfDocument.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(page)));
            }
            return text.toString();
        }
        finally {
            pdfDocument.close();
        }
    }
}
