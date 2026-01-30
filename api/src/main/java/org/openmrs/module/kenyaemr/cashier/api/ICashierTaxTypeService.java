package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;

public interface ICashierTaxTypeService extends IEntityDataService<CashierTaxType> {
	CashierTaxType getByConceptUuid(String conceptUuid);
}
