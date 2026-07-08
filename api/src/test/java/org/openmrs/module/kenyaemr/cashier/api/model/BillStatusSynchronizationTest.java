package org.openmrs.module.kenyaemr.cashier.api.model;

import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class BillStatusSynchronizationTest {

	static {
		File appDataDir = new File("target/openmrs-test-appdata");
		if (!appDataDir.exists()) {
			appDataDir.mkdirs();
		}
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDir.getAbsolutePath());
	}

	@Test
	public void synchronizePaymentStatus_shouldMarkLineItemPaidWhenDiscountConsumesFullAmount() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.valueOf(2000));

		lineItem.synchronizePaymentStatus();

		assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
		assertEquals(0, lineItem.getNetTotal().compareTo(BigDecimal.ZERO));
	}

	@Test
	public void synchronizeBillStatus_shouldMarkBillPaidWhenDiscountsReduceTotalToZero() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.valueOf(2000));
		lineItem.synchronizePaymentStatus();

		Bill bill = new Bill();
		bill.setLineItems(Arrays.asList(lineItem));
		lineItem.setBill(bill);

		bill.synchronizeBillStatus();

		assertEquals(BillStatus.PAID, bill.getStatus());
		assertEquals(BillStatus.PAID, lineItem.getPaymentStatus());
		assertEquals(0, bill.getBalance().compareTo(BigDecimal.ZERO));
	}

	@Test
	public void synchronizePaymentStatus_shouldKeepLineItemPendingWhenPositiveBalanceHasNoAllocations() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.ZERO);

		lineItem.synchronizePaymentStatus();

		assertEquals(BillStatus.PENDING, lineItem.getPaymentStatus());
	}

	@Test
	public void synchronizeBillStatus_shouldKeepEmptyBillPending() {
		Bill bill = new Bill();

		bill.synchronizeBillStatus();

		assertEquals(BillStatus.PENDING, bill.getStatus());
	}

	@Test
	public void getBalance_shouldIgnoreLegacyAdditionalDiscountWhenLineDiscountExists() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(300), BigDecimal.valueOf(50));
		Bill bill = createBill(lineItem);
		bill.setAdditionalDiscount(BigDecimal.valueOf(50));

		bill.synchronizeBillStatus();

		assertEquals(0, bill.getTotal().compareTo(BigDecimal.valueOf(250)));
		assertEquals(0, bill.getAdditionalDiscount().compareTo(BigDecimal.ZERO));
		assertEquals(0, bill.getBalance().compareTo(BigDecimal.valueOf(250)));
		assertEquals(BillStatus.PENDING, bill.getStatus());
	}

	@Test
	public void synchronizeBillStatus_shouldMarkBillPaidWhenPaymentCoversLineTotalWithLegacyAdditionalDiscount() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(300), BigDecimal.valueOf(50));
		Bill bill = createBill(lineItem);
		bill.setAdditionalDiscount(BigDecimal.valueOf(50));

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

	@Test
	public void getAdditionalDiscountEligibleAmount_shouldUsePendingAndPostedUnpaidLineAmountsOnly() {
		BillLineItem pendingLine = createLineItemWithStatus(BigDecimal.valueOf(100), BillStatus.PENDING, BigDecimal.ZERO);
		BillLineItem postedLine = createLineItemWithStatus(BigDecimal.valueOf(200), BillStatus.POSTED, BigDecimal.valueOf(50));
		BillLineItem paidLine = createLineItemWithStatus(BigDecimal.valueOf(300), BillStatus.PAID, BigDecimal.ZERO);
		BillLineItem exemptedLine = createLineItemWithStatus(BigDecimal.valueOf(400), BillStatus.EXEMPTED, BigDecimal.ZERO);
		Bill bill = createBill(pendingLine, postedLine, paidLine, exemptedLine);

		assertEquals(0, bill.getAdditionalDiscountEligibleAmount().compareTo(BigDecimal.valueOf(250)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void validateAdditionalDiscountAmount_shouldRejectDiscountGreaterThanEligibleUnpaidAmount() {
		BillLineItem lineItem = createLineItemWithStatus(BigDecimal.valueOf(100), BillStatus.PENDING, BigDecimal.ZERO);
		Bill bill = createBill(lineItem);

			bill.validateAdditionalDiscountAmount(BigDecimal.valueOf(101));
		}

		@Test
		public void updateAdditionalDiscount_shouldNormalizeLegacyAmountWithoutChangingBalance() {
			BillLineItem lineItem = createLineItemWithStatus(BigDecimal.valueOf(300), BillStatus.PENDING, BigDecimal.ZERO);
			Bill bill = createBill(lineItem);
			bill.setAdditionalDiscount(BigDecimal.valueOf(100));
			assertEquals(0, bill.getAdditionalDiscount().compareTo(BigDecimal.ZERO));

			Payment payment = new Payment();
			payment.setAmount(BigDecimal.valueOf(200));
			payment.setAmountTendered(BigDecimal.valueOf(200));
			payment.setVoided(false);
			payment.setBill(bill);
			bill.setPayments(Collections.singleton(payment));

			bill.synchronizeBillStatus();
			assertEquals(BillStatus.POSTED, bill.getStatus());
			assertEquals(0, bill.getBalance().compareTo(BigDecimal.valueOf(100)));

			bill.updateAdditionalDiscount(BigDecimal.ZERO);

			assertEquals(0, bill.getAdditionalDiscount().compareTo(BigDecimal.ZERO));
			assertEquals(0, bill.getBalance().compareTo(BigDecimal.valueOf(100)));
			assertEquals(BillStatus.POSTED, bill.getStatus());
		}

		@Test
		public void synchronizeStatuses_shouldRefreshLineItemAndBillStatusFromAllocations() {
		BillLineItem lineItem = createLineItemWithDiscount(BigDecimal.valueOf(2000), BigDecimal.ZERO);
		lineItem.setPaymentStatus(BillStatus.PENDING);

		LinePaymentAllocation allocation = new LinePaymentAllocation();
		allocation.setAllocatedAmount(BigDecimal.valueOf(2000));

		Bill bill = new Bill();
		bill.setLineItems(Arrays.asList(lineItem));
		lineItem.setBill(bill);

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

		if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
			BillLineItemAdjustment discount = new BillLineItemAdjustment();
			discount.setAdjustmentType("DISCOUNT");
			discount.setAmount(discountAmount);
			lineItem.setAdjustments(Arrays.asList(discount));
		}

		return lineItem;
	}

	private Bill createBill(BillLineItem... lineItems) {
		Bill bill = new Bill();
		bill.setLineItems(Arrays.asList(lineItems));
		for (BillLineItem lineItem : lineItems) {
			lineItem.setBill(bill);
		}
		return bill;
	}

	private BillLineItem createLineItemWithStatus(BigDecimal price, BillStatus status, BigDecimal allocatedAmount) {
		BillLineItem lineItem = new BillLineItem();
		lineItem.setPrice(price);
		lineItem.setQuantity(1);
		lineItem.setPaymentStatus(status);

		if (allocatedAmount.compareTo(BigDecimal.ZERO) > 0) {
			LinePaymentAllocation allocation = new LinePaymentAllocation();
			allocation.setAllocatedAmount(allocatedAmount);
			lineItem.setAllocations(Collections.singleton(allocation));
		}

		return lineItem;
	}
}
