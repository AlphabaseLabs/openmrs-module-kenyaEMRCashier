package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class BrandingConfigurationProviderTest {

	@Test
	public void shouldUseConfiguredBillingNoteVisibility() throws Exception {
		assertFalse(BrandingConfigurationProvider.resolveShowBillingNote("{\"showBillingNote\":false}"));
		assertTrue(BrandingConfigurationProvider.resolveShowBillingNote("{\"showBillingNote\":true}"));
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
