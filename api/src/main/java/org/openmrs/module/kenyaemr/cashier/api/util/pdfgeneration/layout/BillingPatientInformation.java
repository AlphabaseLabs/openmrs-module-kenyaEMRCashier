package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import java.util.EnumSet;
import java.util.Set;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.openmrs.Patient;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.PdfGenerationUtils;

/** Shared patient fields and labels for billing documents, including thermal receipts. */
public final class BillingPatientInformation {

    public enum Field {
        NAME("name", "Name"), MR_NUMBER("mrNumber", "MR #"), AGE("age", "Age"),
        GENDER("gender", "Gender"), PHONE("phone", "Phone"), ADDRESS("address", "Address");

        private final String key;
        private final String label;

        Field(String key, String label) {
            this.key = key;
            this.label = label;
        }

        public String getKey() {
            return key;
        }
    }

    private BillingPatientInformation() {
    }

    public static Cell createCell(Patient patient, Set<Field> fields) {
        Cell cell = PrintablePdfStyle.detailCell();
        if (!fields.isEmpty()) {
            cell.add(PrintablePdfStyle.sectionHeading("Patient information"));
        }
        for (Field field : fields) {
            cell.add(PrintablePdfStyle.inlineInfoLine(field.label, value(patient, field), field == Field.NAME));
        }
        return cell;
    }

    public static void addReceiptRows(Table table, Patient patient, PdfFont regular) {
        for (Field field : EnumSet.of(Field.NAME, Field.MR_NUMBER)) {
            table.addCell(new Paragraph(field.label + ":").setFont(regular).setFontSize(12));
            table.addCell(new Paragraph(value(patient, field)).setFont(regular).setFontSize(12));
        }
    }

    private static String value(Patient patient, Field field) {
        if (patient == null) {
            return "";
        }
        switch (field) {
            case NAME:
                return PdfGenerationUtils.getPatientName(patient);
            case MR_NUMBER:
                return PdfGenerationUtils.getPatientIdentifier(patient);
            case AGE:
                Integer age = patient.getAge();
                return age == null ? "" : age.toString();
            case GENDER:
                return PdfGenerationUtils.getPatientGender(patient);
            case PHONE:
                return PdfGenerationUtils.getPatientPhoneNumber(patient);
            case ADDRESS:
                return PdfGenerationUtils.formatPatientAddress(patient.getPersonAddress());
            default:
                throw new IllegalArgumentException("Unsupported patient field: " + field);
        }
    }
}
