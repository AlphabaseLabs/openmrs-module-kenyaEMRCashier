package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.invoice;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfDocumentService;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BrandingConfigurationProvider;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.PrintablePdfStyle;

import java.util.List;

public class InvoiceContentSection implements PdfDocumentService.ContentSection {

    private static final float TABLE_MARGIN = 10f;
    private static final float SUMMARY_MARGIN = 8f;

    @Override
    public void render(Document doc, Object data) {
        Bill bill = PdfGenerationUtils.requireBill(data);
        PdfGenerationUtils.CurrencyFormatter currency = new PdfGenerationUtils.CurrencyFormatter();

        createBillItemsTable(doc, bill, currency);
        createTableSummary(doc, bill, currency);
        createPaymentTable(doc, bill, currency);
        createTenderedSummary(doc, bill, currency);
        createBillNote(doc, bill);
    }

    private void createBillItemsTable(Document doc, Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        boolean showDiscount = PdfGenerationUtils.isPositive(bill.getTotalDiscount());
        boolean showTax = PdfGenerationUtils.isPositive(bill.getTotalTax());
        float[] itemColWidths = getItemColumnWidths(showDiscount, showTax);
        Table itemsTable = new Table(UnitValue.createPercentArray(itemColWidths))
                .useAllAvailableWidth()
                .setMarginBottom(TABLE_MARGIN)
                .setKeepTogether(false);

        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Description"));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Quantity"));
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Unit price"));
        if (showDiscount) {
            itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Discount"));
        }
        if (showTax) {
            itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Tax"));
        }
        itemsTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Total"));

        for (BillLineItem item : PdfGenerationUtils.getActiveLineItems(bill)) {
            itemsTable.addCell(PrintablePdfStyle.tableCell(PdfGenerationUtils.getItemDescription(item)));
            itemsTable.addCell(PrintablePdfStyle.tableCell(PdfGenerationUtils.formatQuantity(item.getQuantity())));
            itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getPrice())));
            if (showDiscount) {
                itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getTotalDiscount())));
            }
            if (showTax) {
                itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getTotalTax())));
            }
            itemsTable.addCell(PrintablePdfStyle.tableCell(currency.formatAmount(item.getNetTotal())));
        }

        doc.add(itemsTable);
    }

    private void createTableSummary(Document doc, Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        if (PdfGenerationUtils.isPositive(bill.getTotalDiscount())
                || PdfGenerationUtils.isPositive(bill.getTotalTax())) {
            addCalculationSummaryBlock(doc, bill, currency);
            return;
        }

        addSummaryRow(doc, "Total amount:", currency.formatCurrency(bill.getTotal()));
    }

    private float[] getItemColumnWidths(boolean showDiscount, boolean showTax) {
        if (showDiscount && showTax) {
            return new float[] { 34f, 12f, 18f, 12f, 11f, 13f };
        }
        if (showDiscount) {
            return new float[] { 38f, 14f, 20f, 13f, 15f };
        }
        if (showTax) {
            return new float[] { 38f, 14f, 20f, 12f, 16f };
        }
        return new float[] { 38f, 18f, 24f, 20f };
    }

    private void createPaymentTable(Document doc, Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        List<Payment> payments = PdfGenerationUtils.getActivePaymentsChronologically(bill);
        if (payments.isEmpty()) {
            return;
        }

        Table paymentTable = new Table(UnitValue.createPercentArray(new float[] { 34f, 36f, 30f }))
                .useAllAvailableWidth()
                .setMarginTop(0)
                .setMarginBottom(TABLE_MARGIN);

        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Date of payment"));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Payment method"));
        paymentTable.addHeaderCell(PrintablePdfStyle.tableHeaderCell("Amount paid", TextAlignment.RIGHT));

        for (Payment payment : payments) {
            paymentTable.addCell(
                    PrintablePdfStyle.tableCell(PdfGenerationUtils.formatPaymentDate(payment.getDateCreated())));
            paymentTable.addCell(PrintablePdfStyle.tableCell(PdfGenerationUtils.getPaymentMethod(payment)));
            paymentTable.addCell(PrintablePdfStyle.tableCell(currency.formatCurrency(payment.getAmountTendered()),
                    TextAlignment.RIGHT));
        }

        doc.add(paymentTable);
    }

    private void createTenderedSummary(Document doc, Bill bill, PdfGenerationUtils.CurrencyFormatter currency) {
        Table summaryTable = PrintablePdfStyle.compactSummaryTable(SUMMARY_MARGIN);

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Total tendered:",
                currency.formatCurrency(bill.getTotalActualPayments()), false);
        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Balance:",
                currency.formatCurrency(bill.getBalance()), true);

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

    private void addSummaryRow(Document doc, String label, String value) {
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 70f, 30f }))
                .useAllAvailableWidth()
                .setMarginBottom(SUMMARY_MARGIN);

        summaryTable.addCell(PrintablePdfStyle.summaryLabelCell(label));
        summaryTable.addCell(PrintablePdfStyle.summaryValueCell(value));

        doc.add(summaryTable);
    }

    private void addCalculationSummaryBlock(Document doc, Bill bill,
            PdfGenerationUtils.CurrencyFormatter currency) {
        Table summaryTable = PrintablePdfStyle.compactSummaryTable(SUMMARY_MARGIN);

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Subtotal:",
                currency.formatCurrency(bill.getSubTotal()), false);

        if (PdfGenerationUtils.isPositive(bill.getTotalDiscount())) {
            PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Discount:",
                    "- " + currency.formatCurrency(bill.getTotalDiscount()), false);
        }

        if (PdfGenerationUtils.isPositive(bill.getTotalTax())) {
            PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Tax:",
                    currency.formatCurrency(bill.getTotalTax()), false);
        }

        PrintablePdfStyle.addCompactSummaryRow(summaryTable, "Total amount:",
                currency.formatCurrency(bill.getTotal()), true);

        doc.add(summaryTable);
    }
}
