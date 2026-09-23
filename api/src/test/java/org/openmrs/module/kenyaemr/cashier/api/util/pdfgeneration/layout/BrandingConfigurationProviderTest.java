package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BillingPatientInformation.Field;

public class BrandingConfigurationProviderTest {

	@Test
	public void shouldUseInvoicePatientFieldsWhenNoSelectionExists() throws Exception {
		for (String json : new String[] { null, "", "  ", "{}", "{\"billingPatientInformation\":null}",
		        "{\"billingPatientInformation\":[]}" }) {
			assertEquals(EnumSet.of(Field.NAME, Field.MR_NUMBER, Field.PHONE, Field.ADDRESS),
			    BrandingConfigurationProvider.resolveInvoicePatientFields(json));
		}
	}

	@Test
	public void shouldApplyPatientFieldSelectionIndependentlyOfBillNote() throws Exception {
		Set<Field> fields = BrandingConfigurationProvider.resolveInvoicePatientFields(
		    "{\"showBillingNote\":false,\"billingPatientInformation\":{\"phone\":false,\"address\":false,\"age\":true,\"gender\":true}}");
		assertEquals(EnumSet.of(Field.NAME, Field.MR_NUMBER, Field.AGE, Field.GENDER), fields);
	}

	@Test
	public void shouldAllowAllPatientFieldsToBeHidden() throws Exception {
		assertTrue(BrandingConfigurationProvider.resolveInvoicePatientFields(
		    "{\"billingPatientInformation\":{\"name\":false,\"mrNumber\":false,\"age\":false,\"gender\":false,\"phone\":false,\"address\":false}}")
		        .isEmpty());
	}

	@Test
	public void shouldIgnoreInvalidPatientFieldsWithoutResettingValidSelections() throws Exception {
		Set<Field> fields = BrandingConfigurationProvider.resolveInvoicePatientFields(
		    "{\"billingPatientInformation\":{\"name\":\"false\",\"phone\":false,\"other\":true}}");
		assertTrue(fields.contains(Field.NAME));
		assertFalse(fields.contains(Field.PHONE));
	}

	@Test
	public void shouldUseConfiguredBillingNoteVisibility() throws Exception {
		assertFalse(BrandingConfigurationProvider.resolveShowBillingNote("{\"showBillingNote\":false}"));
		assertTrue(BrandingConfigurationProvider.resolveShowBillingNote("{\"showBillingNote\":true}"));
	}

	@Test
	public void shouldShowAllBillStatementFieldsUnlessExplicitlyEnabled() throws Exception {
		assertEquals(EnumSet.allOf(Field.class), BrandingConfigurationProvider.resolveBillStatementPatientFields(""));
		for (String setting : new String[] { "", "\"applyPatientInformationToBillStatement\":false,",
		        "\"applyPatientInformationToBillStatement\":\"true\",",
		        "\"applyPatientInformationToBillStatement\":null," }) {
			String configuration = "{" + setting + "\"billingPatientInformation\":{\"name\":false,\"phone\":false}}";
			assertEquals(EnumSet.allOf(Field.class),
			    BrandingConfigurationProvider.resolveBillStatementPatientFields(configuration));
			assertFalse(BrandingConfigurationProvider.resolveInvoicePatientFields(configuration).contains(Field.NAME));
		}
	}

	@Test
	public void shouldApplyInvoiceSelectionToBillStatementOnlyWhenEnabled() throws Exception {
		String configuration = "{\"applyPatientInformationToBillStatement\":true,"
		        + "\"billingPatientInformation\":{\"name\":false,\"phone\":false,\"age\":true}}";
		assertEquals(EnumSet.of(Field.MR_NUMBER, Field.AGE, Field.ADDRESS),
		    BrandingConfigurationProvider.resolveBillStatementPatientFields(configuration));
	}

	@Test
	public void shouldUseInvoiceDefaultsWhenStatementOptInHasNoSelection() throws Exception {
		assertEquals(EnumSet.of(Field.NAME, Field.MR_NUMBER, Field.PHONE, Field.ADDRESS),
		    BrandingConfigurationProvider.resolveBillStatementPatientFields(
		        "{\"applyPatientInformationToBillStatement\":true}"));
	}

	@Test
	public void shouldReturnIndependentFieldSelections() throws Exception {
		Set<Field> fields = BrandingConfigurationProvider.resolveInvoicePatientFields("{}");
		fields.clear();
		assertEquals(EnumSet.of(Field.NAME, Field.MR_NUMBER, Field.PHONE, Field.ADDRESS),
		    BrandingConfigurationProvider.resolveInvoicePatientFields("{}"));
		BrandingConfigurationProvider.resolveBillStatementPatientFields("{}").clear();
		assertEquals(EnumSet.allOf(Field.class), BrandingConfigurationProvider.resolveBillStatementPatientFields("{}"));
	}

	@Test(expected = IOException.class)
	public void shouldRejectMalformedInvoiceConfiguration() throws Exception {
		BrandingConfigurationProvider.resolveInvoicePatientFields("{");
	}

	@Test(expected = IOException.class)
	public void shouldRejectNonObjectStatementConfiguration() throws Exception {
		BrandingConfigurationProvider.resolveBillStatementPatientFields("[]");
	}

	@Test
	public void shouldAllowOptedInBillStatementsToHideAllPatientFields() throws Exception {
		assertTrue(BrandingConfigurationProvider.resolveBillStatementPatientFields(
		    "{\"applyPatientInformationToBillStatement\":true,\"billingPatientInformation\":{\"name\":false,\"mrNumber\":false,\"age\":false,\"gender\":false,\"phone\":false,\"address\":false}}")
		        .isEmpty());
	}

	@Test
	public void shouldShowBillingNoteByDefault() throws Exception {
		assertTrue(BrandingConfigurationProvider.resolveShowBillingNote(""));
		assertTrue(BrandingConfigurationProvider.resolveShowBillingNote("{}"));
		assertTrue(BrandingConfigurationProvider.resolveShowBillingNote("{\"showBillingNote\":\"false\"}"));
	}

	@Test(expected = IOException.class)
	public void shouldRejectNonObjectConfiguration() throws Exception {
		BrandingConfigurationProvider.resolveShowBillingNote("[]");
	}
}
