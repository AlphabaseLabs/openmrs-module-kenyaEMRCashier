package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

public class BillStatementContentSectionTest {

    @Test
    public void render_shouldAppendBillNoteAfterStatementSummary() throws IOException {
        Bill bill = new Bill() {
            @Override
            public BigDecimal getBalance() {
                return BigDecimal.ZERO;
            }
        };
        BillLineItem lineItem = new BillLineItem();
        lineItem.setPrice(new BigDecimal("100.00"));
        lineItem.setQuantity(1);
        lineItem.setVoided(false);

        bill.setPatient(new Patient());
        bill.setLineItems(Arrays.asList(lineItem));
        bill.setPayments(new HashSet<Payment>());
        bill.setNote("Statement note for the patient.");

        File pdf = new PdfDocumentServiceImpl().generatePdf("bill-statement-note-test", bill, null,
                new BillStatementContentSection(), null);

        try {
            String text = extractText(pdf);
            assertTrue(text.contains("Note:"));
            assertTrue(text.contains("Statement note for the patient."));
            assertTrue(text.indexOf("Balance due:") < text.indexOf("Note:"));
        }
        finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
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
