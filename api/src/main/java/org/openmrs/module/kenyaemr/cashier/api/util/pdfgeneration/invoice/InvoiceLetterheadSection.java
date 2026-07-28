package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.openmrs.Patient;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.DocumentHeader;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PrintablePdfStyle;

public class InvoiceLetterheadSection implements PdfDocumentService.LetterheadSection {

    private static final float DETAIL_SECTION_BOTTOM_MARGIN = 20f;

    private final DocumentHeader documentHeader = new DocumentHeader();

    @Override
    public void render(Document doc, Object data) {
        Bill bill = PdfGenerationUtils.requireBill(data);
        PdfGenerationUtils.requirePatient(bill);

        documentHeader.setTitle("Invoice").render(doc);
        addInvoiceDetails(doc, bill);
    }

    private void addInvoiceDetails(Document doc, Bill bill) {
        PdfGenerationUtils.CurrencyFormatter currency = new PdfGenerationUtils.CurrencyFormatter();
        Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 66f, 34f }))
                .useAllAvailableWidth()
                .setBorderBottom(new SolidBorder(PrintablePdfStyle.BORDER, 0.5f))
                .setMarginTop(0)
                .setMarginBottom(DETAIL_SECTION_BOTTOM_MARGIN);

        headerTable.addCell(createPatientInfoCell(bill.getPatient()));
        headerTable.addCell(createInvoiceSummaryCell(bill, currency));
        doc.add(headerTable);
    }

    private Cell createPatientInfoCell(Patient patient) {
        Cell cell = PrintablePdfStyle.detailCell();
        cell.add(PrintablePdfStyle.sectionHeading("Patient information"));
        cell.add(PrintablePdfStyle.inlineInfoLine("Name", PdfGenerationUtils.getPatientName(patient), true));
        cell.add(PrintablePdfStyle.inlineInfoLine("MR #", PdfGenerationUtils.getPatientIdentifier(patient)));
        cell.add(PrintablePdfStyle.inlineInfoLine("Phone", PdfGenerationUtils.getPatientPhoneNumber(patient)));
        cell.add(PrintablePdfStyle.inlineInfoLine("Address",
                PdfGenerationUtils.formatPatientAddress(patient.getPersonAddress())));
        return cell;
    }

    private Cell createInvoiceSummaryCell(Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        Cell cell = PrintablePdfStyle.detailCell();
        cell.add(PrintablePdfStyle.sectionHeading("Invoice summary"));
        cell.add(PrintablePdfStyle.infoLine("Invoice #", bill.getReceiptNumber()));
        cell.add(PrintablePdfStyle.infoLine("Invoice date",
                PdfGenerationUtils.formatDocumentDate(bill.getDateCreated())));
        cell.add(PrintablePdfStyle.infoLine("Total amount", currency.formatCurrency(bill.getTotal())));
        cell.add(PrintablePdfStyle.infoLine("Total paid",
                currency.formatCurrency(bill.getTotalActualPayments())));
        cell.add(PrintablePdfStyle.infoLine("Amount balance", currency.formatCurrency(bill.getBalance())));
        cell.add(PrintablePdfStyle.emphasizedInfoLine("Invoice status",
                bill.getStatus() != null ? bill.getStatus().toString() : "", 4f));
        return cell;
    }
}
