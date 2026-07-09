package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;

public interface PaymentAllocationRebalanceService extends OpenmrsService {

	void rebalancePaymentAllocations(Bill bill, BillLineItem changedLineItem);
}
