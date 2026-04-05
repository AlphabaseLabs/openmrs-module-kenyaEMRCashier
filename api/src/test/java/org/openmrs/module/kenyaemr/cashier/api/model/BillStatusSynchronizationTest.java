package org.openmrs.module.kenyaemr.cashier.api.model;

import org.junit.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;

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
}
