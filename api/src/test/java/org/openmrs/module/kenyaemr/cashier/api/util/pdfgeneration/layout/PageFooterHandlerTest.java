package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageFooterHandlerTest {

    @Test
    public void formatThankYouMessage_shouldReplaceFacilityPlaceholders() {
        String template = "Thank you for choosing {facilityName}. We wish you good health. "
                + "For billing inquiries, contact our finance department at {facilityTel}.";

        String message = PageFooterHandler.formatThankYouMessage(template, "Alphabase Clinic", "+923047294971");

        assertEquals("Thank you for choosing Alphabase Clinic. We wish you good health. "
                + "For billing inquiries, contact our finance department at +923047294971.", message);
    }
}
