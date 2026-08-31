package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class BrandingLogoProviderTest {

	@Test
	public void shouldPreferConfiguredPath() throws Exception {
		String location = BrandingLogoProvider.resolveLogoLocation(
		    "{\"logoPath\":\" /data/logo.png \",\"logoUrl\":\"https://example.com/logo.png\"}");
		assertEquals("/data/logo.png", location);
	}

	@Test
	public void shouldFallbackToConfiguredUrl() throws Exception {
		assertEquals("https://example.com/logo.png",
		    BrandingLogoProvider.resolveLogoLocation("{\"logoUrl\":\"https://example.com/logo.png\"}"));
		assertEquals("", BrandingLogoProvider.resolveLogoLocation(""));
	}

	@Test(expected = IOException.class)
	public void shouldRejectNonObjectConfiguration() throws Exception {
		BrandingLogoProvider.resolveLogoLocation("[]");
	}
}
