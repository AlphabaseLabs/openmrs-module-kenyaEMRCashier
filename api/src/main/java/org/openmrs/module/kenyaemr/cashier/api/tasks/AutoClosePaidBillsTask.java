/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.scheduler.tasks.AbstractTask;

import java.util.List;

/**
 * Scheduled task that closes paid bills that are still open.
 */
public class AutoClosePaidBillsTask extends AbstractTask {
	private static final Log LOG = LogFactory.getLog(AutoClosePaidBillsTask.class);

	@Override
	public void execute() {
		if (isExecuting) {
			return;
		}

		startExecuting();
		Context.addProxyPrivilege(PrivilegeConstants.VIEW_BILLS);
		Context.addProxyPrivilege(PrivilegeConstants.CLOSE_BILLS);

		try {
			if (!ModuleSettings.isAutoClosePaidBillsEnabled()) {
				return;
			}

			IBillService billService = Context.getService(IBillService.class);
			BillSearch billSearch = createBillSearch();
			List<Bill> bills = billService.getBills(billSearch);
			String closeReason = ModuleSettings.getAutoClosePaidBillsReason();

			int closedCount = 0;
			for (Bill bill : bills) {
				try {
					if (bill != null && bill.canBeClosed()) {
						billService.closeBill(bill, closeReason);
						closedCount++;
					}
				} catch (Exception e) {
					LOG.error("Failed to auto-close bill " + (bill != null ? bill.getUuid() : "unknown"), e);
				}
			}

			if (LOG.isInfoEnabled()) {
				LOG.info("AutoClosePaidBillsTask completed. Closed " + closedCount + " bill(s).");
			}
		} catch (Exception e) {
			LOG.error("Error while auto closing paid bills:", e);
		} finally {
			Context.removeProxyPrivilege(PrivilegeConstants.CLOSE_BILLS);
			Context.removeProxyPrivilege(PrivilegeConstants.VIEW_BILLS);
			stopExecuting();
		}
	}

	BillSearch createBillSearch() {
		Bill template = new Bill();
		template.setStatus(BillStatus.PAID);

		BillSearch billSearch = new BillSearch(template, false);
		billSearch.setIncludeClosedBills(false);
		return billSearch;
	}
}
