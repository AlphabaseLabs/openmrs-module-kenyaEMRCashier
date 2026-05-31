package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.openmrs.module.kenyaemr.cashier.api.ICashierItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ICashierItemPriceServiceImpl extends BaseEntityDataServiceImpl<CashierItemPrice> implements IEntityAuthorizationPrivileges
        , ICashierItemPriceService {
    @Override
    protected IEntityAuthorizationPrivileges getPrivileges() {
        return this;
    }

    @Override
    protected void validate(CashierItemPrice object) {

    }

    @Override
    public String getVoidPrivilege() {
        return PrivilegeConstants.MANAGE_METADATA;
    }

    @Override
    public String getSavePrivilege() {
        return PrivilegeConstants.MANAGE_METADATA;
    }

    @Override
    public String getPurgePrivilege() {
        return PrivilegeConstants.PURGE_METADATA;
    }

    @Override
    public String getGetPrivilege() {
        return PrivilegeConstants.VIEW_METADATA;
    }
}
