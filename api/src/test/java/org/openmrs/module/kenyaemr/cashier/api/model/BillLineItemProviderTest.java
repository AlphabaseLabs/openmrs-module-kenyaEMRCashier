package org.openmrs.module.kenyaemr.cashier.api.model;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.openmrs.Provider;

public class BillLineItemProviderTest {

	@Test
	public void shouldAllowAssigningAndClearingAProvider() {
		BillLineItem lineItem = new BillLineItem();
		Provider provider = new Provider();

		assertNull(lineItem.getProvider());

		lineItem.setProvider(provider);
		assertSame(provider, lineItem.getProvider());

		lineItem.setProvider(null);
		assertNull(lineItem.getProvider());
	}
}
