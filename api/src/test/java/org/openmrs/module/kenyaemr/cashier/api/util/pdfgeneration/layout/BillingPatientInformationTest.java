package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.Property;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonName;
import org.openmrs.module.kenyaemr.cashier.api.impl.BillServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BillingPatientInformation.Field;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BillingPatientInformationTest {

    @Test
    public void createCell_shouldOmitHeadingWhenNoFieldsAreSelected() {
        assertTrue(BillingPatientInformation.createCell(new Patient(), EnumSet.noneOf(Field.class)).getChildren().isEmpty());
    }

    @Test
    public void createCell_shouldRenderOnlySelectedFieldsInOrder() {
        Cell cell = BillingPatientInformation.createCell(patient(), EnumSet.of(Field.MR_NUMBER, Field.GENDER));

        assertEquals(3, cell.getChildren().size());
        assertEquals("Patient information", text((Paragraph) cell.getChildren().get(0), 0));
        Paragraph identifier = (Paragraph) cell.getChildren().get(1);
        assertEquals("MR #: ", text(identifier, 0));
        assertEquals("MR-123", text(identifier, 1));
        Paragraph gender = (Paragraph) cell.getChildren().get(2);
        assertEquals("Gender: ", text(gender, 0));
        assertEquals("Female", text(gender, 1));
    }

    @Test
    public void createCell_shouldRenderMissingDemographicsAsBlank() {
        Cell cell = BillingPatientInformation.createCell(new Patient(), EnumSet.of(Field.AGE, Field.ADDRESS));

        assertEquals("", text((Paragraph) cell.getChildren().get(1), 1));
        assertEquals("", text((Paragraph) cell.getChildren().get(2), 1));
    }

    @Test
    public void addReceiptRows_shouldUseRegularFontAndOnlyNameAndMrNumber() throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        Table table = new Table(2);
        BillingPatientInformation.addReceiptRows(table, patient(), font);

        assertEquals(2, table.getNumberOfRows());
        String[][] expected = { { "Name:", "Sample Patient" }, { "MR #:", "MR-123" } };
        for (int row = 0; row < expected.length; row++) {
            for (int column = 0; column < expected[row].length; column++) {
                Paragraph paragraph = (Paragraph) table.getCell(row, column).getChildren().get(0);
                assertEquals(expected[row][column], text(paragraph, 0));
                assertSame(font, paragraph.getProperty(Property.FONT));
                assertNull(paragraph.getProperty(Property.BOLD_SIMULATION));
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void downloadBillReceipt_shouldRejectMissingPatientBeforeGeneratingDocument() {
        new BillServiceImpl().downloadBillReceipt(new Bill());
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.addName(new PersonName("Sample", null, "Patient"));
        patient.setGender("F");
        PatientIdentifier identifier = new PatientIdentifier();
        identifier.setIdentifier("MR-123");
        identifier.setPreferred(true);
        patient.addIdentifier(identifier);
        return patient;
    }

    private String text(Paragraph paragraph, int index) {
        return ((Text) paragraph.getChildren().get(index)).getText();
    }
}
