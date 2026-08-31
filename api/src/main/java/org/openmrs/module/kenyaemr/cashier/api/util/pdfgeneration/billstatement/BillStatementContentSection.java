package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.billstatement;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BrandingConfigurationProvider;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PrintablePdfStyle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class BillStatementContentSection implements PdfDocumentService.ContentSection {

    private static final float TABLE_MARGIN = 8f;
    private static final float SECTION_SPACING = 10f;

    @Override
    public void render(Document doc, Object data) {
        Bill bill = PdfGenerationUtils.requireBill(data);
        PdfGenerationUtils.requirePatient(bill);
        PdfGenerationUtils.requireLineItems(bill);
        PdfGenerationUtils.CurrencyFormatter currency = new PdfGenerationUtils.CurrencyFormatter();

        createDetailedBillItemsTable(doc, bill, currency);
        createPaymentHistoryTable(doc, bill, currency);
        createBillSummary(doc, bill, currency);
        createBillNote(doc, bill);
    }

    private void createDetailedBillItemsTable(Document doc, Bill bill,
            PdfGenerationUtils.CurrencyFormatter currency) {
        doc.add(PrintablePdfStyle.sectionHeading("Detailed list of services/items provided"));

        boolean showDiscount = PdfGenerationUtils.isPositive(bill.getTotalDiscount());
        boolean showTax = PdfGenerationUtils.isPositive(bill.getTotalTax());
        float[] itemColWidths = getStatementItemColumnWidths(showDiscount, showTax);
        Table itemsTable = new Table(UnitValue.createPercentArray(itemColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN)
                .setKeepTogether(false);

        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("No", TextAlignment.CENTER));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Service/Item description"));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Qty", TextAlignment.CENTER));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Unit price"));
        if (showDiscount) {
            itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Discount"));
        }
        if (showTax) {
            itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Tax"));
        }
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Total"));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Date added", TextAlignment.CENTER));

        int itemNumber = 1;
        for (BillLineItem item : PdfGenerationUtils.getActiveLineItemsChronologically(bill)) {
            itemsTable.addCell(PrintablePdfStyle.tableCell(String.valueOf(itemNumber++), TextAlignment.CENTER));
            itemsTable.addCell(PrintablePdfStyle.tableCell(PdfGenerationUtils.getItemDescription(item)));
            itemsTable.addCell(
                    PrintablePdfStyle.tableCell(PdfGenerationUtils.formatQuantity(item.getQuantity()),
                            TextAlignment.CENTER));
            itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getPrice())));
            if (showDiscount) {
                itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getTotalDiscount())));
            }
            if (showTax) {
                itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getTotalTax())));
            }
            itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getNetTotal())));
            itemsTable.addCell(
                    PrintablePdfStyle.tableCell(PdfGenerationUtils.formatLineItemDate(item.getDateCreated()),
                            TextAlignment.CENTER));
        }

        doc.add(itemsTable);
    }

    private void createPaymentHistoryTable(Document doc, Bill bill,
            PdfGenerationUtils.CurrencyFormatter currency) {
        doc.add(PrintablePdfStyle.sectionHeading("Payment history"));

        List<Payment> payments = PdfGenerationUtils.getActivePaymentsChronologically(bill);
        if (payments.isEmpty()) {
            doc.add(PrintablePdfStyle.emptyState("No payments recorded for this bill.", SECTION_SPACING));
            return;
        }

        float[] paymentColWidths = { 6f, 16f, 16f, 16f, 16f, 16f, 13f };
        Table paymentTable = new Table(UnitValue.createPercentArray(paymentColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN);

        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("No", TextAlignment.CENTER));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Date", TextAlignment.CENTER));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Method", TextAlignment.CENTER));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Tendered", TextAlignment.RIGHT));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Applied", TextAlignment.RIGHT));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Cashier", TextAlignment.CENTER));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Reference", TextAlignment.CENTER));

        int paymentNumber = 1;
        for (Payment payment : payments) {
            paymentTable.addCell(PrintablePdfStyle.tableCell(String.valueOf(paymentNumber++), TextAlignment.CENTER));
            paymentTable.addCell(
                    PrintablePdfStyle.tableCell(PdfGenerationUtils.formatTimestamp(payment.getDateCreated()),
                            TextAlignment.CENTER));
            paymentTable.addCell(
                    PrintablePdfStyle.tableCell(PdfGenerationUtils.getPaymentMethod(payment), TextAlignment.CENTER));
            paymentTable.addCell(
                    PrintablePdfStyle.tableCell(currency.formatCurrency(payment.getAmountTendered()),
                            TextAlignment.RIGHT));
            paymentTable.addCell(
                    PrintablePdfStyle.tableCell(currency.formatCurrency(payment.getAmount()),
                            TextAlignment.RIGHT));
            paymentTable.addCell(PrintablePdfStyle.tableCell(getCashierDisplay(payment), TextAlignment.CENTER));
            paymentTable.addCell(PrintablePdfStyle.tableCell(getPaymentReference(payment), TextAlignment.CENTER));
        }

        doc.add(paymentTable);
    }

    private void createBillSummary(Document doc, Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        doc.add(PrintablePdfStyle.sectionHeading("Bill summary"));

        Table summaryTable = PrintablePdfStyle.compactSummaryTable(TABLE_MARGIN);
        BigDecimal subtotalAmount = bill.getSubTotal();
        BigDecimal totalDiscount = bill.getTotalDiscount();
        BigDecimal totalTax = bill.getTotalTax();
        BigDecimal totalBillAmount = bill.getTotal();
        BigDecimal totalActualPayments = bill.getTotalActualPayments();
        BigDecimal totalWaivers = bill.getTotalWaivers();
        BigDecimal balanceDue = bill.getBalance();

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Subtotal:",
                currency.formatCurrency(subtotalAmount), false);

        if (PdfGenerationUtils.isPositive(totalDiscount)) {
            PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Discount:",
                    "- " + currency.formatCurrency(totalDiscount), false);
        }

        if (PdfGenerationUtils.isPositive(totalTax)) {
            PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Tax:",
                    currency.formatCurrency(totalTax), false);
        }

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Total bill amount:",
                currency.formatCurrency(totalBillAmount), true);
        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Total paid:",
                currency.formatCurrency(totalActualPayments), true, 4f);

        if (PdfGenerationUtils.isPositive(totalWaivers)) {
            PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Total waived:",
                    currency.formatCurrency(totalWaivers), false);
        }

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Balance due:",
                currency.formatCurrency(balanceDue), true);

        doc.add(summaryTable);
    }

    private void createBillNote(Document doc, Bill bill) {
        if (!BrandingConfigurationProvider.shouldShowBillingNote()) {
            return;
        }
        String note = StringUtils.trimToNull(bill.getNote());
        if (note != null) {
            doc.add(PrintablePdfStyle.noteBlock(note));
        }
    }

    private float[] getStatementItemColumnWidths(boolean showDiscount, boolean showTax) {
        if (showDiscount && showTax) {
            return new float[] { 4f, 30f, 7f, 11f, 10f, 9f, 11f, 17f };
        }
        if (showDiscount) {
            return new float[] { 4f, 34f, 7f, 13f, 11f, 13f, 18f };
        }
        if (showTax) {
            return new float[] { 4f, 34f, 7f, 13f, 10f, 14f, 18f };
        }
        return new float[] { 4f, 40f, 8f, 15f, 15f, 18f };
    }

    private String getPaymentReference(Payment payment) {
        Set<PaymentAttribute> attributes = payment.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (PaymentAttribute attr : attributes) {
                if (attr != null && attr.getValue() != null) {
                    String value = attr.getValue().trim();
                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        return "-";
    }

    private String getCashierDisplay(Payment payment) {
        if (payment == null || payment.getCreator() == null) {
            return "N/A";
        }

        User user = payment.getCreator();

        if (user.getPerson() != null && user.getPerson().getPersonName() != null) {
            String fullName = user.getPerson().getPersonName().getFullName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName;
            }
        }

        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        }

        if (user.getSystemId() != null && !user.getSystemId().isEmpty()) {
            return user.getSystemId();
        }

        if (user.getId() != null) {
            return "User #" + user.getId();
        }

        return "N/A";
    }
}
