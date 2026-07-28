package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.impl.PdfDocumentServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.CarbonPdfFonts;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PdfGenerationUtilsTest {

    @Test
    public void getActiveLineItemsChronologically_shouldFilterVoidedItemsWithoutMutatingBill() {
        BillLineItem newer = lineItem(2_000L, false);
        BillLineItem older = lineItem(1_000L, false);
        BillLineItem voided = lineItem(500L, true);
        List<BillLineItem> originalOrder = new ArrayList<BillLineItem>(Arrays.asList(newer, voided, older));
        Bill bill = new Bill();
        bill.setLineItems(originalOrder);

        List<BillLineItem> result = PdfGenerationUtils.getActiveLineItemsChronologically(bill);

        assertEquals(2, result.size());
        assertSame(older, result.get(0));
        assertSame(newer, result.get(1));
        assertSame(newer, bill.getLineItems().get(0));
        assertSame(voided, bill.getLineItems().get(1));
        assertSame(older, bill.getLineItems().get(2));
    }

    @Test
    public void getActivePaymentsChronologically_shouldReturnDeterministicActivePayments() {
        Payment newer = payment(2_000L, false);
        Payment older = payment(1_000L, false);
        Payment voided = payment(500L, true);
        Bill bill = new Bill();
        bill.setPayments(new HashSet<Payment>(Arrays.asList(newer, older, voided)));

        List<Payment> result = PdfGenerationUtils.getActivePaymentsChronologically(bill);

        assertEquals(2, result.size());
        assertSame(older, result.get(0));
        assertSame(newer, result.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireBill_shouldRejectUnsupportedData() {
        PdfGenerationUtils.requireBill("not a bill");
    }

    @Test
    public void openDocumentScope_shouldReuseEmbeddedFontsWithinDocument() {
        try (CarbonPdfFonts.FontScope ignored = CarbonPdfFonts.openDocumentScope()) {
            assertSame(CarbonPdfFonts.regular(), CarbonPdfFonts.regular());
            assertSame(CarbonPdfFonts.semibold(), CarbonPdfFonts.semibold());
            assertEquals("IBMPlexSans", CarbonPdfFonts.regular().getFontProgram().getFontNames().getFontName());
            assertEquals("IBMPlexSans-SmBld", CarbonPdfFonts.semibold().getFontProgram().getFontNames().getFontName());
        }
    }

    @Test
    public void generatePdf_shouldCreateReadableDocumentWithEmbeddedFonts() throws IOException {
        File pdf = new PdfDocumentServiceImpl().generatePdf("pdf-test", null, null,
                new PdfDocumentService.ContentSection() {
                    @Override
                    public void render(Document doc, Object data) {
                        doc.add(new Paragraph("Regular").setFont(CarbonPdfFonts.regular()));
                        doc.add(new Paragraph("Semibold").setFont(CarbonPdfFonts.semibold()));
                    }
                }, null);

        try {
            assertTrue(pdf.length() > 0);
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdf));
            try {
                assertEquals(1, pdfDocument.getNumberOfPages());
            } finally {
                pdfDocument.close();
            }
        } finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    @Test
    public void generatePdf_shouldRenderPaidStampForFullyPaidBills() throws IOException {
        File pdf = new PdfDocumentServiceImpl().generatePdf("paid-stamp-test", paidBill(BillStatus.PAID), null,
                bodyContent(), null);

        try {
            assertTrue(getFirstPageText(pdf).contains("PAID"));
        } finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    @Test
    public void generatePdf_shouldNotRenderPaidStampForOpenBills() throws IOException {
        File pdf = new PdfDocumentServiceImpl().generatePdf("open-bill-stamp-test", paidBill(BillStatus.POSTED), null,
                bodyContent(), null);

        try {
            assertTrue(!getFirstPageText(pdf).contains("PAID"));
        } finally {
            assertTrue(pdf.delete() || !pdf.exists());
        }
    }

    private PdfDocumentService.ContentSection bodyContent() {
        return new PdfDocumentService.ContentSection() {
            @Override
            public void render(Document doc, Object data) {
                doc.add(new Paragraph("Bill body"));
            }
        };
    }

    private String getFirstPageText(File pdf) throws IOException {
        PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdf));
        try {
            return PdfTextExtractor.getTextFromPage(pdfDocument.getFirstPage());
        } finally {
            pdfDocument.close();
        }
    }

    private Bill paidBill(BillStatus status) {
        Bill bill = new Bill();
        bill.setStatus(status);
        bill.setLineItems(new ArrayList<BillLineItem>(Arrays.asList(lineItemWithPrice("100.00"))));
        bill.setPayments(new HashSet<Payment>(Arrays.asList(paymentWithAmount("100.00"))));
        return bill;
    }

    private BillLineItem lineItemWithPrice(String amount) {
        BillLineItem item = new BillLineItem();
        item.setPrice(new BigDecimal(amount));
        item.setQuantity(1);
        item.setVoided(false);
        return item;
    }

    private Payment paymentWithAmount(String amount) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal(amount));
        payment.setAmountTendered(new BigDecimal(amount));
        payment.setVoided(false);
        return payment;
    }

    private BillLineItem lineItem(long timestamp, boolean voided) {
        BillLineItem item = new BillLineItem();
        item.setDateCreated(new Date(timestamp));
        item.setVoided(voided);
        return item;
    }

    private Payment payment(long timestamp, boolean voided) {
        Payment payment = new Payment();
        payment.setDateCreated(new Date(timestamp));
        payment.setVoided(voided);
        return payment;
    }
}
