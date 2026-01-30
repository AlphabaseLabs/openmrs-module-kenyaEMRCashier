package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.kenyaemr.cashier.api.ICashierTaxTypeService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ICashierTaxTypeServiceImpl extends BaseEntityDataServiceImpl<CashierTaxType>
        implements IEntityAuthorizationPrivileges, ICashierTaxTypeService {
	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	protected void validate(CashierTaxType object) {
	}

	@Override
	public String getVoidPrivilege() {
		return null;
	}

	@Override
	public String getSavePrivilege() {
		return null;
	}

	@Override
	public String getPurgePrivilege() {
		return null;
	}

	@Override
	public String getGetPrivilege() {
		return null;
	}

	@Override
	public CashierTaxType getByConceptUuid(String conceptUuid) {
		if (conceptUuid == null) {
			return null;
		}
		Criteria criteria = getRepository().createCriteria(CashierTaxType.class);
		criteria.createAlias("concept", "concept");
		criteria.add(Restrictions.eq("concept.uuid", conceptUuid));
		return (CashierTaxType) criteria.uniqueResult();
	}
}
