package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryMetricsSummary {
	private BigDecimal totalPayments = BigDecimal.ZERO;
	private BigDecimal cash = BigDecimal.ZERO;
	private BigDecimal others = BigDecimal.ZERO;
	private String topPayeeName;
	private BigDecimal topPayeeAmount = BigDecimal.ZERO;
	private List<PaymentMethodTotalSummary> paymentMethodTotals = new ArrayList<PaymentMethodTotalSummary>();

	public BigDecimal getTotalPayments() {
		return totalPayments;
	}

	public void setTotalPayments(BigDecimal totalPayments) {
		this.totalPayments = totalPayments;
	}

	public BigDecimal getCash() {
		return cash;
	}

	public void setCash(BigDecimal cash) {
		this.cash = cash;
	}

	public BigDecimal getOthers() {
		return others;
	}

	public void setOthers(BigDecimal others) {
		this.others = others;
	}

	public String getTopPayeeName() {
		return topPayeeName;
	}

	public void setTopPayeeName(String topPayeeName) {
		this.topPayeeName = topPayeeName;
	}

	public BigDecimal getTopPayeeAmount() {
		return topPayeeAmount;
	}

	public void setTopPayeeAmount(BigDecimal topPayeeAmount) {
		this.topPayeeAmount = topPayeeAmount;
	}

	public List<PaymentMethodTotalSummary> getPaymentMethodTotals() {
		return paymentMethodTotals;
	}

	public void setPaymentMethodTotals(List<PaymentMethodTotalSummary> paymentMethodTotals) {
		this.paymentMethodTotals = paymentMethodTotals == null ? new ArrayList<PaymentMethodTotalSummary>()
		        : paymentMethodTotals;
	}
}
