package org.openmrs.module.kenyaemr.cashier.api.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;

public class AutoClosePaidBillsTaskTest {

	static {
		File appDataDir = new File("target/openmrs-test-appdata");
		if (!appDataDir.exists()) {
			appDataDir.mkdirs();
		}
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDir.getAbsolutePath());
	}

	@Test
	public void createBillSearch_shouldTargetAllOpenPaidBills() {
		AutoClosePaidBillsTask task = new AutoClosePaidBillsTask();

		BillSearch search = task.createBillSearch();

		assertEquals(BillStatus.PAID, search.getTemplate().getStatus());
		assertFalse(search.getIncludeClosedBills());
		assertNull(search.getCreatedOnOrAfter());
		assertNull(search.getCreatedOnOrBefore());
	}
}
