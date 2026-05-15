package org.openmrs.module.kenyaemr.cashier.api.model;

import java.math.BigDecimal;

public class PaymentMethodTotalSummary {
	private String paymentMethod;
	private BigDecimal total;

	public PaymentMethodTotalSummary() {
	}

	public PaymentMethodTotalSummary(String paymentMethod, BigDecimal total) {
		this.paymentMethod = paymentMethod;
		this.total = total;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}
}
