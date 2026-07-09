package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.junit.Test;
import org.openmrs.User;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BillServiceImplPaymentRebalanceTest {

	static {
		File appDataDir = new File("target/openmrs-test-appdata");
		if (!appDataDir.exists()) {
			appDataDir.mkdirs();
		}
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDir.getAbsolutePath());
	}

	private final User user = new User(1);
	private final PaymentAllocationRebalanceServiceImpl paymentAllocationRebalanceService =
	        new PaymentAllocationRebalanceServiceImpl() {
		@Override
		protected User getAuthenticatedUser() {
			return user;
		}
	};

	@Test
	public void rebalancePaymentAllocations_shouldWrapLeftoverReleasedAmountToEarlierEligibleLine() {
		BillLineItem earlierLine = createLineItem("earlier-line", 1500, BillStatus.PENDING, 3);
		BillLineItem changedLine = createLineItem("changed-line", 0, BillStatus.PAID, 8);
		BillLineItem laterLine = createLineItem("later-line", 450, BillStatus.POSTED, 9);
		Bill bill = createBill(earlierLine, changedLine, laterLine);

		Payment payment = createPayment("Cash", 500);
		addPayment(bill, payment);
		LinePaymentAllocation changedAllocation = addAllocation(bill, payment, changedLine, 450, 1000L);
		LinePaymentAllocation laterExistingAllocation = addAllocation(bill, payment, laterLine, 50, 2000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertTrue(changedAllocation.getVoided());
		assertFalse(laterExistingAllocation.getVoided());
		assertAmount(0, changedLine.getTotalAllocated());
		assertAmount(450, laterLine.getTotalAllocated());
		assertAmount(50, earlierLine.getTotalAllocated());
		assertAmount(500, payment.getTotalAllocated());
		assertEquals(BillStatus.PAID, changedLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, laterLine.getPaymentStatus());
		assertEquals(BillStatus.POSTED, earlierLine.getPaymentStatus());
		assertEquals(BillStatus.POSTED, bill.getStatus());
	}

	@Test
	public void rebalancePaymentAllocations_shouldUseLineItemOrderInsteadOfCollectionOrder() {
		BillLineItem changedLine = createLineItem("changed-line", 0, BillStatus.PAID, 2);
		BillLineItem earlierLine = createLineItem("earlier-line", 100, BillStatus.PENDING, 1);
		BillLineItem laterLine = createLineItem("later-line", 80, BillStatus.PENDING, 3);
		Bill bill = createBill(changedLine, earlierLine, laterLine);

		Payment payment = createPayment("Cash", 80);
		addPayment(bill, payment);
		addAllocation(bill, payment, changedLine, 80, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(0, earlierLine.getTotalAllocated());
		assertAmount(80, laterLine.getTotalAllocated());
		assertEquals(BillStatus.PENDING, earlierLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, laterLine.getPaymentStatus());
	}

	@Test
	public void rebalancePaymentAllocations_shouldLeaveReleasedAmountUnallocatedWhenNoRecipientHasRemainingBalance() {
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		BillLineItem fullyAllocatedLine = createLineItem("fully-allocated-line", 40, BillStatus.PAID);
		BillLineItem exemptedLine = createLineItem("exempted-line", 100, BillStatus.EXEMPTED);
		BillLineItem voidedLine = createLineItem("voided-line", 100, BillStatus.PENDING);
		voidedLine.setVoided(true);
		Bill bill = createBill(changedLine, fullyAllocatedLine, exemptedLine, voidedLine);

		Payment payment = createPayment("Cash", 140);
		addPayment(bill, payment);
		LinePaymentAllocation changedAllocation = addAllocation(bill, payment, changedLine, 100, 1000L);
		LinePaymentAllocation fullyAllocatedLineAllocation = addAllocation(bill, payment, fullyAllocatedLine, 40, 2000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(60, changedAllocation.getAllocatedAmount());
		assertFalse(changedAllocation.getVoided());
		assertSameAllocation(fullyAllocatedLine, fullyAllocatedLineAllocation);
		assertNoAllocations(exemptedLine);
		assertNoAllocations(voidedLine);
		assertAmount(100, payment.getTotalAllocated());
	}

	@Test
	public void rebalancePaymentAllocations_shouldTrimChangedLineAndCascadeReleasedAmountToNextLaterLine() {
		BillLineItem earlierLine = createLineItem("earlier-line", 50, BillStatus.PAID);
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		BillLineItem laterLine = createLineItem("later-line", 50, BillStatus.PENDING);
		Bill bill = createBill(earlierLine, changedLine, laterLine);

		Payment payment = createPayment("Cash", 150);
		addPayment(bill, payment);
		LinePaymentAllocation earlierAllocation = addAllocation(bill, payment, earlierLine, 50, 1000L);
		LinePaymentAllocation changedAllocation = addAllocation(bill, payment, changedLine, 100, 2000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(50, earlierAllocation.getAllocatedAmount());
		assertFalse(earlierAllocation.getVoided());
		assertAmount(60, changedAllocation.getAllocatedAmount());
		assertFalse(changedAllocation.getVoided());
		assertAmount(60, changedLine.getTotalAllocated());
		assertAmount(40, laterLine.getTotalAllocated());
		assertEquals(BillStatus.PAID, changedLine.getPaymentStatus());
		assertEquals(BillStatus.POSTED, laterLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, earlierLine.getPaymentStatus());
		assertEquals(BillStatus.POSTED, bill.getStatus());
	}

	@Test
	public void rebalancePaymentAllocations_shouldNotMoveEarlierLineAllocations() {
		BillLineItem earlierLine = createLineItem("earlier-line", 50, BillStatus.PAID);
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		BillLineItem laterLine = createLineItem("later-line", 50, BillStatus.PENDING);
		Bill bill = createBill(earlierLine, changedLine, laterLine);

		Payment payment = createPayment("Cash", 150);
		addPayment(bill, payment);
		LinePaymentAllocation earlierAllocation = addAllocation(bill, payment, earlierLine, 50, 1000L);
		addAllocation(bill, payment, changedLine, 100, 2000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertSameAllocation(earlierLine, earlierAllocation);
		assertAmount(50, earlierLine.getTotalAllocated());
		assertAmount(40, laterLine.getTotalAllocated());
	}

	@Test
	public void rebalancePaymentAllocations_shouldDoNothingWhenChangedLineIsNotOverallocated() {
		BillLineItem changedLine = createLineItem("changed-line", 100, BillStatus.POSTED);
		BillLineItem laterLine = createLineItem("later-line", 50, BillStatus.PENDING);
		Bill bill = createBill(changedLine, laterLine);

		Payment payment = createPayment("Cash", 80);
		addPayment(bill, payment);
		LinePaymentAllocation changedAllocation = addAllocation(bill, payment, changedLine, 80, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(80, changedAllocation.getAllocatedAmount());
		assertFalse(changedAllocation.getVoided());
		assertNoAllocations(laterLine);
	}

	@Test
	public void rebalancePaymentAllocations_shouldNotUseWaiverOrVoidedPaymentsForReleasedValue() {
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		BillLineItem laterLine = createLineItem("later-line", 100, BillStatus.PENDING);
		Bill bill = createBill(changedLine, laterLine);

		Payment cashPayment = createPayment("Cash", 70);
		Payment waiverPayment = createPayment("Waiver", 30);
		Payment voidedPayment = createPayment("Cash", 30);
		voidedPayment.setVoided(true);
		addPayment(bill, cashPayment);
		addPayment(bill, waiverPayment);
		addPayment(bill, voidedPayment);
		LinePaymentAllocation cashAllocation = addAllocation(bill, cashPayment, changedLine, 70, 3000L);
		LinePaymentAllocation waiverAllocation = addAllocation(bill, waiverPayment, changedLine, 30, 2000L);
		LinePaymentAllocation voidedPaymentAllocation = addAllocation(bill, voidedPayment, changedLine, 30, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertTrue(cashAllocation.getVoided());
		assertFalse(waiverAllocation.getVoided());
		assertFalse(voidedPaymentAllocation.getVoided());
		assertAmount(30, waiverAllocation.getAllocatedAmount());
		assertAmount(30, voidedPaymentAllocation.getAllocatedAmount());
		assertAmount(70, laterLine.getTotalAllocated());
		for (LinePaymentAllocation allocation : laterLine.getAllocations()) {
			assertEquals(cashPayment, allocation.getPayment());
		}
	}

	@Test
	public void rebalancePaymentAllocations_shouldSkipVoidedAndTerminalLaterLines() {
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		BillLineItem voidedLine = createLineItem("voided-line", 100, BillStatus.PENDING);
		voidedLine.setVoided(true);
		BillLineItem exemptedLine = createLineItem("exempted-line", 100, BillStatus.EXEMPTED);
		BillLineItem cancelledLine = createLineItem("cancelled-line", 100, BillStatus.CANCELLED);
		BillLineItem adjustedLine = createLineItem("adjusted-line", 100, BillStatus.ADJUSTED);
		BillLineItem eligibleLine = createLineItem("eligible-line", 100, BillStatus.PENDING);
		Bill bill = createBill(changedLine, voidedLine, exemptedLine, cancelledLine, adjustedLine, eligibleLine);

		Payment payment = createPayment("Cash", 130);
		addPayment(bill, payment);
		addAllocation(bill, payment, changedLine, 100, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertNoAllocations(voidedLine);
		assertNoAllocations(exemptedLine);
		assertNoAllocations(cancelledLine);
		assertNoAllocations(adjustedLine);
		assertAmount(40, eligibleLine.getTotalAllocated());
	}

	@Test
	public void rebalancePaymentAllocations_shouldSynchronizeLineItemAndBillStatusesAfterCascade() {
		BillLineItem changedLine = createLineItem("changed-line", 50, BillStatus.POSTED);
		BillLineItem laterLine = createLineItem("later-line", 50, BillStatus.PENDING);
		Bill bill = createBill(changedLine, laterLine);
		bill.setStatus(BillStatus.PENDING);

		Payment payment = createPayment("Cash", 100);
		addPayment(bill, payment);
		addAllocation(bill, payment, changedLine, 100, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(50, changedLine.getTotalAllocated());
		assertAmount(50, laterLine.getTotalAllocated());
		assertEquals(BillStatus.PAID, changedLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, laterLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, bill.getStatus());
	}

	@Test
	public void rebalancePaymentAllocations_shouldCascadeBackwardToEarlierLinesWhenChangedLineIsLast() {
		BillLineItem earlierLine = createLineItem("earlier-line", 100, BillStatus.PENDING);
		BillLineItem changedLine = createLineItem("changed-line", 60, BillStatus.PAID);
		Bill bill = createBill(earlierLine, changedLine);

		Payment payment = createPayment("Cash", 100);
		addPayment(bill, payment);
		addAllocation(bill, payment, changedLine, 100, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(60, changedLine.getTotalAllocated());
		assertAmount(40, earlierLine.getTotalAllocated());
		assertEquals(BillStatus.PAID, changedLine.getPaymentStatus());
		assertEquals(BillStatus.POSTED, earlierLine.getPaymentStatus());
	}

	@Test
	public void rebalancePaymentAllocations_shouldCascadeForwardThenBackwardWhenForwardLinesInsufficient() {
		BillLineItem earlierLine = createLineItem("earlier-line", 30, BillStatus.PENDING);
		BillLineItem changedLine = createLineItem("changed-line", 20, BillStatus.PAID);
		BillLineItem laterLine = createLineItem("later-line", 10, BillStatus.PENDING);
		Bill bill = createBill(earlierLine, changedLine, laterLine);

		Payment payment = createPayment("Cash", 100);
		addPayment(bill, payment);
		addAllocation(bill, payment, changedLine, 80, 1000L);

		paymentAllocationRebalanceService.rebalancePaymentAllocations(bill, changedLine);

		assertAmount(20, changedLine.getTotalAllocated());
		assertAmount(10, laterLine.getTotalAllocated());
		assertAmount(30, earlierLine.getTotalAllocated());
		assertEquals(BillStatus.PAID, changedLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, laterLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, earlierLine.getPaymentStatus());
		assertEquals(BillStatus.PAID, bill.getStatus());
	}

	private Bill createBill(BillLineItem... lineItems) {
		Bill bill = new Bill();
		bill.setVoided(false);
		bill.setStatus(BillStatus.PENDING);
		List<BillLineItem> billLineItems = new ArrayList<BillLineItem>();
		for (BillLineItem lineItem : lineItems) {
			billLineItems.add(lineItem);
			lineItem.setBill(bill);
		}
		bill.setLineItems(billLineItems);
		return bill;
	}

	private BillLineItem createLineItem(String uuid, int netTotal, BillStatus status) {
		BillLineItem lineItem = new BillLineItem();
		lineItem.setUuid(uuid);
		lineItem.setVoided(false);
		lineItem.setPrice(BigDecimal.valueOf(netTotal));
		lineItem.setQuantity(1);
		lineItem.setPaymentStatus(status);
		lineItem.setAllocations(new HashSet<LinePaymentAllocation>());
		return lineItem;
	}

	private BillLineItem createLineItem(String uuid, int netTotal, BillStatus status, int lineItemOrder) {
		BillLineItem lineItem = createLineItem(uuid, netTotal, status);
		lineItem.setLineItemOrder(lineItemOrder);
		return lineItem;
	}

	private Payment createPayment(String modeName, int amount) {
		PaymentMode mode = new PaymentMode();
		mode.setName(modeName);

		Payment payment = new Payment();
		payment.setUuid(modeName + "-" + amount + "-" + System.nanoTime());
		payment.setCreator(user);
		payment.setInstanceType(mode);
		payment.setAmount(BigDecimal.valueOf(amount));
		payment.setAmountTendered(BigDecimal.valueOf(amount));
		payment.setVoided(false);
		return payment;
	}

	private void addPayment(Bill bill, Payment payment) {
		if (bill.getPayments() == null) {
			bill.setPayments(new HashSet<Payment>());
		}
		bill.getPayments().add(payment);
		payment.setBill(bill);
	}

	private LinePaymentAllocation addAllocation(Bill bill, Payment payment, BillLineItem lineItem, int amount,
	        long dateCreatedMillis) {
		LinePaymentAllocation allocation = new LinePaymentAllocation();
		allocation.setUuid("allocation-" + amount + "-" + dateCreatedMillis + "-" + System.nanoTime());
		allocation.setBill(bill);
		allocation.setPayment(payment);
		allocation.setBillLineItem(lineItem);
		allocation.setAllocatedAmount(BigDecimal.valueOf(amount));
		allocation.setCreator(user);
		allocation.setDateCreated(new Date(dateCreatedMillis));
		allocation.setVoided(false);
		payment.addAllocation(allocation);
		lineItem.addAllocation(allocation);
		return allocation;
	}

	private void assertAmount(int expected, BigDecimal actual) {
		assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual));
	}

	private void assertNoAllocations(BillLineItem lineItem) {
		assertTrue(lineItem.getAllocations().isEmpty());
	}

	private void assertSameAllocation(BillLineItem lineItem, LinePaymentAllocation expectedAllocation) {
		assertNotNull(lineItem.getAllocations());
		assertEquals(Collections.singleton(expectedAllocation), lineItem.getAllocations());
	}
}
