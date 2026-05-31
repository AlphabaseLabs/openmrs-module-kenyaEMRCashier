package org.openmrs.module.kenyaemr.cashier.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface IBillableItemsService extends IEntityDataService<BillableService> {
    @Authorized({ PrivilegeConstants.VIEW_METADATA })
    List<BillableService> findServices(final BillableServiceSearch search);
}
