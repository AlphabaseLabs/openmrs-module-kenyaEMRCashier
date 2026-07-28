package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

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

public class BillStatementLetterheadSection implements PdfDocumentService.LetterheadSection {

    private static final float DETAIL_SECTION_BOTTOM_MARGIN = 20f;

    private final DocumentHeader documentHeader = new DocumentHeader();

    @Override
    public void render(Document doc, Object data) {
        Bill bill = PdfGenerationUtils.requireBill(data);
        PdfGenerationUtils.requirePatient(bill);

        documentHeader.setTitle("Bill Statement").render(doc);
        addBillDetails(doc, bill);
    }

    private void addBillDetails(Document doc, Bill bill) {
        PdfGenerationUtils.CurrencyFormatter currency = new PdfGenerationUtils.CurrencyFormatter();
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 66f, 34f }))
                .useAllAvailableWidth()
                .setBorderBottom(new SolidBorder(PrintablePdfStyle.BORDER, 0.5f))
                .setMarginTop(0)
                .setMarginBottom(DETAIL_SECTION_BOTTOM_MARGIN);

        summaryTable.addCell(createPatientInfoCell(bill.getPatient()));
        summaryTable.addCell(createBillSummaryCell(bill, currency));
        doc.add(summaryTable);
    }

    private Cell createPatientInfoCell(Patient patient) {
        Cell cell = PrintablePdfStyle.detailCell();
        cell.add(PrintablePdfStyle.sectionHeading("Patient information"));
        cell.add(PrintablePdfStyle.inlineInfoLine("Name", PdfGenerationUtils.getPatientName(patient), true));
        cell.add(PrintablePdfStyle.inlineInfoLine("MR #", PdfGenerationUtils.getPatientIdentifier(patient)));
        cell.add(PrintablePdfStyle.inlineInfoLine("Age",
                patient.getAge() != null ? patient.getAge().toString() : ""));
        cell.add(PrintablePdfStyle.inlineInfoLine("Gender", PdfGenerationUtils.getPatientGender(patient)));
        cell.add(PrintablePdfStyle.inlineInfoLine("Phone", PdfGenerationUtils.getPatientPhoneNumber(patient)));
        cell.add(PrintablePdfStyle.inlineInfoLine("Address",
                PdfGenerationUtils.formatPatientAddress(patient.getPersonAddress())));
        return cell;
    }

    private Cell createBillSummaryCell(Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        Cell cell = PrintablePdfStyle.detailCell();
        cell.add(PrintablePdfStyle.sectionHeading("Bill summary"));
        cell.add(PrintablePdfStyle.infoLine("Bill #", bill.getReceiptNumber()));
        cell.add(PrintablePdfStyle.infoLine("Bill date",
                PdfGenerationUtils.formatDocumentDate(bill.getDateCreated())));
        cell.add(PrintablePdfStyle.infoLine("Total amount", currency.formatCurrency(bill.getTotal())));
        cell.add(PrintablePdfStyle.infoLine("Total paid",
                currency.formatCurrency(bill.getTotalActualPayments())));
        if (PdfGenerationUtils.isPositive(bill.getTotalWaivers())) {
            cell.add(PrintablePdfStyle.infoLine("Total waived",
                    currency.formatCurrency(bill.getTotalWaivers())));
        }
        cell.add(PrintablePdfStyle.infoLine("Amount balance", currency.formatCurrency(bill.getBalance())));
        cell.add(PrintablePdfStyle.infoLine("Cash point", getCashPointName(bill)));
        cell.add(PrintablePdfStyle.infoLine("Cashier", getCashierName(bill)));
        cell.add(PrintablePdfStyle.emphasizedInfoLine("Bill status",
                bill.getStatus() != null ? bill.getStatus().name() : "UNKNOWN", 4f));
        return cell;
    }

    private String getCashPointName(Bill bill) {
        return bill.getCashPoint() != null ? PdfGenerationUtils.defaultIfBlank(bill.getCashPoint().getName(), "") : "";
    }

    private String getCashierName(Bill bill) {
        return bill.getCashier() != null ? PdfGenerationUtils.defaultIfBlank(bill.getCashier().getName(), "") : "";
    }
}
