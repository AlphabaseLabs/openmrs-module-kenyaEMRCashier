package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;

import org.openmrs.BaseOpenmrsData;

public class BillableServiceTax extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;

	private int serviceTaxId;
	private BillableService billableService;
	private CashierTaxType taxType;
	private BigDecimal overrideRate;
	private Integer priority;

	@Override
	public Integer getId() {
		return serviceTaxId;
	}

	@Override
	public void setId(Integer id) {
		this.serviceTaxId = (id == null) ? 0 : id;
	}

	public int getServiceTaxId() {
		return serviceTaxId;
	}

	public void setServiceTaxId(int serviceTaxId) {
		this.serviceTaxId = serviceTaxId;
	}

	public BillableService getBillableService() {
		return billableService;
	}

	public void setBillableService(BillableService billableService) {
		this.billableService = billableService;
	}

	public CashierTaxType getTaxType() {
		return taxType;
	}

	public void setTaxType(CashierTaxType taxType) {
		this.taxType = taxType;
	}

	public BigDecimal getOverrideRate() {
		return overrideRate;
	}

	public void setOverrideRate(BigDecimal overrideRate) {
		this.overrideRate = overrideRate;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
