package org.openmrs.module.kenyaemr.cashier.api.model;

import junit.framework.TestCase;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

public class BillStatusSynchronizationTest extends TestCase {

	static {
		File appDataDir = new File("target/openmrs-test-appdata");
		appDataDir.mkdirs();
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDir.getAbsolutePath());
	}

	public void testSynchronizePaymentStatus_shouldMarkLineItemPaidWhenDiscountConsumesFullAmount() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.valueOf(2000));

		lineItem.synchronizePaymentStatus();

		assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
		assertEquals(0, lineItem.getNetTotal().compareTo(BigDecimal.ZERO));
	}

	public void testSynchronizeBillStatus_shouldMarkBillPaidWhenDiscountsReduceTotalToZero() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.valueOf(2000));
		lineItem.synchronizePaymentStatus();
		Bill bill = createBill(lineItem);

		bill.synchronizeBillStatus();

		assertEquals(BillStatus.PAID, bill.getStatus());
		assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
		assertEquals(0, bill.getBalance().compareTo(BigDecimal.ZERO));
	}

	public void testSynchronizePaymentStatus_shouldKeepLineItemPendingWhenPositiveBalanceHasNoAllocations() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.ZERO);

		lineItem.synchronizePaymentStatus();

		assertEquals(BillStatus.PENDING, lineItem.getPaymentStatus());
	}

	public void testSynchronizeBillStatus_shouldKeepEmptyBillPending() {
		Bill bill = new Bill();

		bill.synchronizeBillStatus();

		assertEquals(BillStatus.PENDING, bill.getStatus());
	}

	public void testGetBalance_shouldUseLineItemDiscountsOnly() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(300), BigDecimal.valueOf(50));
		Bill bill = createBill(lineItem);

		bill.synchronizeBillStatus();

		assertEquals(0, bill.getTotal().compareTo(BigDecimal.valueOf(250)));
		assertEquals(0, bill.getTotalDiscount().compareTo(BigDecimal.valueOf(50)));
		assertEquals(0, bill.getBalance().compareTo(BigDecimal.valueOf(250)));
		assertEquals(BillStatus.PENDING, bill.getStatus());
	}

	public void testSynchronizeBillStatus_shouldMarkBillPaidWhenPaymentCoversLineTotalAfterDiscounts() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(300), BigDecimal.valueOf(50));
		Bill bill = createBill(lineItem);

		Payment payment = new Payment();
		payment.setAmount(BigDecimal.valueOf(250));
		payment.setAmountTendered(BigDecimal.valueOf(250));
		payment.setVoided(false);
		payment.setBill(bill);
		bill.setPayments(Collections.singleton(payment));

		bill.synchronizeBillStatus();

		assertEquals(0, bill.getBalance().compareTo(BigDecimal.ZERO));
		assertEquals(BillStatus.PAID, bill.getStatus());
	}

	public void testSynchronizeStatuses_shouldRefreshLineItemAndBillStatusFromAllocations() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.ZERO);
		lineItem.setPaymentStatus(BillStatus.PENDING);

		LinePaymentAllocation allocation = new LinePaymentAllocation();
		allocation.setAllocatedAmount(BigDecimal.valueOf(2000));

		Bill bill = createBill(lineItem);

		Payment payment = new Payment();
		payment.setAmount(BigDecimal.valueOf(2000));
		payment.setAmountTendered(BigDecimal.valueOf(2000));
		payment.setVoided(false);
		payment.setAllocations(Collections.singleton(allocation));
		payment.setBill(bill);

		allocation.setBill(bill);
		allocation.setPayment(payment);
		lineItem.setAllocations(Collections.singleton(allocation));
		bill.setPayments(Collections.singleton(payment));
		bill.setStatus(BillStatus.PENDING);

		lineItem.synchronizePaymentStatus();
		bill.synchronizeBillStatus();

		assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
		assertEquals(BillStatus.PAID, bill.getStatus());
		assertEquals(0, bill.getBalance().compareTo(BigDecimal.ZERO));
	}

	private BillLineItem createLineItemWithDiscount(BigDecimal price, BigDecimal discountAmount) {
		BillLineItem lineItem = new BillLineItem();
		lineItem.setPrice(price);
		lineItem.setQuantity(1);
		lineItem.setPaymentStatus(BillStatus.PENDING);

		BillLineItemAdjustment discount = new BillLineItemAdjustment();
		discount.setAdjustmentType("DISCOUNT");
		discount.setAmount(discountAmount);
		lineItem.setAdjustments(Arrays.asList(discount));

		return lineItem;
	}

	private Bill createBill(BillLineItem lineItem) {
		Bill bill = new Bill();
		bill.setLineItems(Collections.singletonList(lineItem));
		lineItem.setBill(bill);
		return bill;
	}
}
