package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;

import java.math.BigDecimal;

/**
 * Model class that represents the allocation of a payment to a specific bill line item.
 */
public class LinePaymentAllocation extends BaseOpenmrsData {
    private Integer linePaymentAllocationId;
    private Bill bill;
    private Payment payment;
    private BillLineItem billLineItem;
    private BigDecimal allocatedAmount;

    @Override
    public Integer getId() {
        return linePaymentAllocationId;
    }

    @Override
    public void setId(Integer id) {
        this.linePaymentAllocationId = id;
    }

    public Integer getLinePaymentAllocationId() {
        return linePaymentAllocationId;
    }

    public void setLinePaymentAllocationId(Integer linePaymentAllocationId) {
        this.linePaymentAllocationId = linePaymentAllocationId;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public BillLineItem getBillLineItem() {
        return billLineItem;
    }

    public void setBillLineItem(BillLineItem billLineItem) {
        this.billLineItem = billLineItem;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
