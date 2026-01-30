package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Provider;

public class BillLineItemAdjustment extends BaseOpenmrsData {
	public static final long serialVersionUID = 0L;

	private int billLineItemAdjustmentId;
	private BillLineItem billLineItem;
	private String adjustmentType;
	private CashierTaxType taxType;
	private BigDecimal rate;
	private BigDecimal amount;
	private BigDecimal baseAmount;
	private String description;
	private Provider discountSponsor;

	@Override
	public Integer getId() {
		return billLineItemAdjustmentId;
	}

	@Override
	public void setId(Integer id) {
		this.billLineItemAdjustmentId = (id == null) ? 0 : id;
	}

	public int getBillLineItemAdjustmentId() {
		return billLineItemAdjustmentId;
	}

	public void setBillLineItemAdjustmentId(int billLineItemAdjustmentId) {
		this.billLineItemAdjustmentId = billLineItemAdjustmentId;
	}

	public BillLineItem getBillLineItem() {
		return billLineItem;
	}

	public void setBillLineItem(BillLineItem billLineItem) {
		this.billLineItem = billLineItem;
	}

	public String getAdjustmentType() {
		return adjustmentType;
	}

	public void setAdjustmentType(String adjustmentType) {
		this.adjustmentType = adjustmentType;
	}

	public CashierTaxType getTaxType() {
		return taxType;
	}

	public void setTaxType(CashierTaxType taxType) {
		this.taxType = taxType;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getBaseAmount() {
		return baseAmount;
	}

	public void setBaseAmount(BigDecimal baseAmount) {
		this.baseAmount = baseAmount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Provider getDiscountSponsor() {
		return discountSponsor;
	}

	public void setDiscountSponsor(Provider discountSponsor) {
		this.discountSponsor = discountSponsor;
	}
}
