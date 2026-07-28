package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration;

import com.fasterxml.jackson.databind.JsonNode;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.util.CurrencyUtil;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfGenerationUtils {

    private static final DateTimeFormatter DOCUMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd - MMM - yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter PAYMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_SYSTEM_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final Comparator<Date> NULLS_LAST_DATE_COMPARATOR =
            Comparator.nullsLast(Comparator.naturalOrder());

    private PdfGenerationUtils() {
    }

    public static Bill requireBill(Object data) {
        if (!(data instanceof Bill)) {
            String actualType = data == null ? "null" : data.getClass().getSimpleName();
            throw new IllegalArgumentException("Expected Bill data but received " + actualType);
        }
        return (Bill) data;
    }

    public static void requirePatient(Bill bill) {
        if (bill.getPatient() == null) {
            throw new IllegalArgumentException("Bill must have an associated patient. Bill ID: " + bill.getId());
        }
    }

    public static void requireLineItems(Bill bill) {
        if (bill.getLineItems() != null) {
            for (BillLineItem item : bill.getLineItems()) {
                if (item != null && !Boolean.TRUE.equals(item.getVoided())) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Bill must have at least one active line item. Bill ID: " + bill.getId());
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static String formatDocumentDate(Date date) {
        return formatDate(date, DOCUMENT_DATE_FORMAT, "");
    }

    public static String formatPaymentDate(Date date) {
        return formatDate(date, PAYMENT_DATE_FORMAT, "-");
    }

    public static String formatLineItemDate(Date date) {
        return formatDate(date, PAYMENT_DATE_FORMAT, "N/A");
    }

    public static String formatTimestamp(Date date) {
        return formatDate(date, TIMESTAMP_FORMAT, "N/A");
    }

    public static String formatSystemTimestamp(Date date) {
        return formatDate(date, TIMESTAMP_FORMAT, "");
    }

    public static String formatSystemDate(Date date) {
        return formatDate(date, SHORT_SYSTEM_DATE_FORMAT, "");
    }

    private static String formatDate(Date date, DateTimeFormatter formatter, String fallback) {
        if (date == null) {
            return fallback;
        }
        return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
    }

    public static List<BillLineItem> getActiveLineItems(Bill bill) {
        List<BillLineItem> items = new ArrayList<BillLineItem>();
        if (bill.getLineItems() == null) {
            return items;
        }

        for (BillLineItem item : bill.getLineItems()) {
            if (item != null && !Boolean.TRUE.equals(item.getVoided())) {
                items.add(item);
            }
        }
        return items;
    }

    public static List<BillLineItem> getActiveLineItemsChronologically(Bill bill) {
        List<BillLineItem> items = getActiveLineItems(bill);
        Collections.sort(items, new Comparator<BillLineItem>() {
            @Override
            public int compare(BillLineItem left, BillLineItem right) {
                return NULLS_LAST_DATE_COMPARATOR.compare(left.getDateCreated(), right.getDateCreated());
            }
        });
        return items;
    }

    public static List<Payment> getActivePaymentsChronologically(Bill bill) {
        List<Payment> payments = new ArrayList<Payment>();
        if (bill.getPayments() == null) {
            return payments;
        }

        for (Payment payment : bill.getPayments()) {
            if (payment != null && !Boolean.TRUE.equals(payment.getVoided())) {
                payments.add(payment);
            }
        }

        Collections.sort(payments, new Comparator<Payment>() {
            @Override
            public int compare(Payment left, Payment right) {
                return NULLS_LAST_DATE_COMPARATOR.compare(left.getDateCreated(), right.getDateCreated());
            }
        });
        return payments;
    }

    public static String getItemDescription(BillLineItem item) {
        if (item.getItem() != null && isNotBlank(item.getItem().getCommonName())) {
            return item.getItem().getCommonName();
        }
        if (item.getBillableService() != null && isNotBlank(item.getBillableService().getName())) {
            return item.getBillableService().getName();
        }
        return "Service/Item";
    }

    public static String formatQuantity(Integer quantity) {
        return String.valueOf(quantity != null ? quantity : 1);
    }

    public static String getPaymentMethod(Payment payment) {
        return payment.getInstanceType() != null && isNotBlank(payment.getInstanceType().getName())
                ? payment.getInstanceType().getName()
                : "Cash";
    }

    public static String getPatientName(Patient patient) {
        return patient != null && patient.getPersonName() != null
                ? nullToEmpty(patient.getPersonName().getFullName())
                : "";
    }

    public static String getPatientIdentifier(Patient patient) {
        if (patient == null) {
            return "";
        }

        PatientIdentifier preferredIdentifier = patient.getPatientIdentifier();
        if (preferredIdentifier != null && isNotBlank(preferredIdentifier.getIdentifier())) {
            return preferredIdentifier.getIdentifier();
        }

        if (patient.getIdentifiers() != null) {
            for (PatientIdentifier identifier : patient.getIdentifiers()) {
                if (identifier != null && isNotBlank(identifier.getIdentifier())) {
                    return identifier.getIdentifier();
                }
            }
        }
        return "";
    }

    public static String getPatientPhoneNumber(Patient patient) {
        if (patient == null || patient.getAttributes() == null) {
            return "";
        }

        for (PersonAttribute attribute : patient.getAttributes()) {
            if (attribute == null || attribute.getAttributeType() == null || !isNotBlank(attribute.getValue())) {
                continue;
            }

            String name = nullToEmpty(attribute.getAttributeType().getName());
            String description = nullToEmpty(attribute.getAttributeType().getDescription());
            String searchableName = (name + " " + description).toLowerCase(Locale.ROOT);
            if (searchableName.contains("phone") || searchableName.contains("telephone")
                    || searchableName.contains("mobile") || searchableName.contains("contact")) {
                return attribute.getValue().trim();
            }
        }
        return "";
    }

    public static String formatPatientAddress(PersonAddress address) {
        if (address == null) {
            return "";
        }

        List<String> parts = new ArrayList<String>();
        addIfPresent(parts, address.getAddress1());
        addIfPresent(parts, address.getCityVillage());
        addIfPresent(parts, address.getCountyDistrict());
        addIfPresent(parts, address.getStateProvince());
        addIfPresent(parts, address.getCountry());
        return String.join(", ", parts);
    }

    public static String getPatientGender(Patient patient) {
        if (patient == null || patient.getGender() == null) {
            return "";
        }
        if ("M".equalsIgnoreCase(patient.getGender())) {
            return "Male";
        }
        if ("F".equalsIgnoreCase(patient.getGender())) {
            return "Female";
        }
        return patient.getGender();
    }

    public static String extractDocumentNumber(Object data) {
        if (data instanceof Bill) {
            return defaultIfBlank(((Bill) data).getReceiptNumber(), "N/A");
        }
        if (data instanceof Map) {
            Map<?, ?> values = (Map<?, ?>) data;
            Object documentNumber = values.get("documentNumber");
            if (documentNumber == null) {
                documentNumber = values.get("receiptNumber");
            }
            return documentNumber != null ? documentNumber.toString() : "N/A";
        }
        if (data instanceof JsonNode) {
            JsonNode json = (JsonNode) data;
            if (json.hasNonNull("documentNumber")) {
                return json.get("documentNumber").asText();
            }
            if (json.hasNonNull("receiptNumber")) {
                return json.get("receiptNumber").asText();
            }
        }
        return "N/A";
    }

    public static String defaultIfBlank(String value, String fallback) {
        return isNotBlank(value) ? value : fallback;
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (isNotBlank(value)) {
            parts.add(value.trim());
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class CurrencyFormatter {

        private final String currencySymbol = CurrencyUtil.getCurrencySymbol();
        private final DecimalFormat decimalFormat = CurrencyUtil.getCurrencyDecimalFormat();

        public String formatAmount(BigDecimal amount) {
            return decimalFormat.format(amount != null ? amount : BigDecimal.ZERO);
        }

        public String formatCurrency(BigDecimal amount) {
            return currencySymbol + " " + formatAmount(amount);
        }
    }
}
