package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;
import java.util.Date;

import org.openmrs.BaseOpenmrsData;

public class BillingHistorySummary extends BaseOpenmrsData {
	private Integer id;
	private String receiptNumber;
	private String patientUuid;
	private String patientName;
	private String identifier;
	private Date dateCreated;
	private String status;
	private BigDecimal totalAmount;
	private BigDecimal totalDiscount;
	private BigDecimal totalPaid;
	private BigDecimal amountDue;
	private String billedItems;
	private String referenceCodes;

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getBillId() {
		return getId();
	}

	public void setBillId(Integer billId) {
		setId(billId);
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String receiptNumber) {
		this.receiptNumber = receiptNumber;
	}

	public String getPatientUuid() {
		return patientUuid;
	}

	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public BigDecimal getTotalDiscount() {
		return totalDiscount;
	}

	public void setTotalDiscount(BigDecimal totalDiscount) {
		this.totalDiscount = totalDiscount;
	}

	public BigDecimal getTotalPaid() {
		return totalPaid;
	}

	public void setTotalPaid(BigDecimal totalPaid) {
		this.totalPaid = totalPaid;
	}

	public BigDecimal getAmountDue() {
		return amountDue;
	}

	public void setAmountDue(BigDecimal amountDue) {
		this.amountDue = amountDue;
	}

	public String getBilledItems() {
		return billedItems;
	}

	public void setBilledItems(String billedItems) {
		this.billedItems = billedItems;
	}

	public String getReferenceCodes() {
		return referenceCodes;
	}

	public void setReferenceCodes(String referenceCodes) {
		this.referenceCodes = referenceCodes;
	}
}
