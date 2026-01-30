package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;

public class CashierTaxType extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;

	private int taxTypeId;
	private Concept concept;
	private BigDecimal rate;
	private Boolean inclusive;

	@Override
	public Integer getId() {
		return taxTypeId;
	}

	@Override
	public void setId(Integer id) {
		this.taxTypeId = (id == null) ? 0 : id;
	}

	public int getTaxTypeId() {
		return taxTypeId;
	}

	public void setTaxTypeId(int taxTypeId) {
		this.taxTypeId = taxTypeId;
	}

	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public Boolean getInclusive() {
		return inclusive;
	}

	public void setInclusive(Boolean inclusive) {
		this.inclusive = inclusive;
	}
}
