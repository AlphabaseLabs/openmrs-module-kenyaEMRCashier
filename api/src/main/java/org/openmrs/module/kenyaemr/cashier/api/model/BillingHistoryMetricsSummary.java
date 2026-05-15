package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillingHistoryMetricsSummary {
	private BigDecimal totalBills = BigDecimal.ZERO;
	private BigDecimal totalPayments = BigDecimal.ZERO;
	private BigDecimal totalDue = BigDecimal.ZERO;
	private BigDecimal totalDiscount = BigDecimal.ZERO;
	private BigDecimal waivedAmount = BigDecimal.ZERO;
	private BigDecimal exemptedAmount = BigDecimal.ZERO;
	private BigDecimal taxCollectionAmount = BigDecimal.ZERO;
	private List<PaymentMethodTotalSummary> paymentMethodTotals = new ArrayList<PaymentMethodTotalSummary>();

	public BigDecimal getTotalBills() {
		return totalBills;
	}

	public void setTotalBills(BigDecimal totalBills) {
		this.totalBills = totalBills;
	}

	public BigDecimal getTotalPayments() {
		return totalPayments;
	}

	public void setTotalPayments(BigDecimal totalPayments) {
		this.totalPayments = totalPayments;
	}

	public BigDecimal getTotalDue() {
		return totalDue;
	}

	public void setTotalDue(BigDecimal totalDue) {
		this.totalDue = totalDue;
	}

	public BigDecimal getTotalDiscount() {
		return totalDiscount;
	}

	public void setTotalDiscount(BigDecimal totalDiscount) {
		this.totalDiscount = totalDiscount;
	}

	public BigDecimal getWaivedAmount() {
		return waivedAmount;
	}

	public void setWaivedAmount(BigDecimal waivedAmount) {
		this.waivedAmount = waivedAmount;
	}

	public BigDecimal getExemptedAmount() {
		return exemptedAmount;
	}

	public void setExemptedAmount(BigDecimal exemptedAmount) {
		this.exemptedAmount = exemptedAmount;
	}

	public BigDecimal getTaxCollectionAmount() {
		return taxCollectionAmount;
	}

	public void setTaxCollectionAmount(BigDecimal taxCollectionAmount) {
		this.taxCollectionAmount = taxCollectionAmount;
	}

	public List<PaymentMethodTotalSummary> getPaymentMethodTotals() {
		return paymentMethodTotals;
	}

	public void setPaymentMethodTotals(List<PaymentMethodTotalSummary> paymentMethodTotals) {
		this.paymentMethodTotals = paymentMethodTotals == null ? new ArrayList<PaymentMethodTotalSummary>()
		        : paymentMethodTotals;
	}
}
