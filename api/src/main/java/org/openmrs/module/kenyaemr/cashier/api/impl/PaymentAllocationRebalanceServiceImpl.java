package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.PaymentAllocationRebalanceService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Transactional
public class PaymentAllocationRebalanceServiceImpl implements PaymentAllocationRebalanceService {

	@Override
	public void onStartup() {
	}

	@Override
	public void onShutdown() {
	}

	@Override
	public void rebalancePaymentAllocations(Bill bill, BillLineItem changedLineItem) {
		if (bill == null || changedLineItem == null || bill.getLineItems() == null) {
			return;
		}

		List<BillLineItem> lineItemsInBillOrder = getLineItemsInBillOrder(bill);
		int changedLineIndex = findLineItemIndex(lineItemsInBillOrder, changedLineItem);
		if (changedLineIndex < 0) {
			return;
		}

		BillLineItem changedLine = lineItemsInBillOrder.get(changedLineIndex);
		BigDecimal excess = changedLine.getTotalAllocated().subtract(changedLine.getNetTotal());
		if (excess.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		List<ReleasedPaymentChunk> releasedChunks = trimChangedLineAllocations(changedLine, excess);
		if (releasedChunks.isEmpty()) {
			return;
		}

		Set<BillLineItem> affectedLineItems = new HashSet<BillLineItem>();
		affectedLineItems.add(changedLine);
		reallocateReleasedChunks(bill, getWrappedRecipientLineItems(lineItemsInBillOrder, changedLineIndex),
		    releasedChunks, affectedLineItems);
		synchronizeAffectedStatuses(bill, affectedLineItems);
	}

	private List<BillLineItem> getLineItemsInBillOrder(Bill bill) {
		List<BillLineItem> lineItemsInBillOrder = new ArrayList<BillLineItem>();
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem != null && !Boolean.TRUE.equals(lineItem.getVoided())) {
				lineItemsInBillOrder.add(lineItem);
			}
		}
		Collections.sort(lineItemsInBillOrder, new Comparator<BillLineItem>() {
			@Override
			public int compare(BillLineItem left, BillLineItem right) {
				return compareLineItemsInBillOrder(left, right);
			}
		});
		return lineItemsInBillOrder;
	}

	private int compareLineItemsInBillOrder(BillLineItem left, BillLineItem right) {
		int orderComparison = compareIntegersAscending(left.getLineItemOrder(), right.getLineItemOrder());
		if (orderComparison != 0) {
			return orderComparison;
		}
		int idComparison = compareIntegersAscending(left.getId(), right.getId());
		if (idComparison != 0) {
			return idComparison;
		}
		return compareStringsAscending(left.getUuid(), right.getUuid());
	}

	private List<BillLineItem> getWrappedRecipientLineItems(List<BillLineItem> lineItemsInBillOrder,
	        int changedLineIndex) {
		List<BillLineItem> recipientLineItems = new ArrayList<BillLineItem>();
		for (int i = changedLineIndex + 1; i < lineItemsInBillOrder.size(); i++) {
			recipientLineItems.add(lineItemsInBillOrder.get(i));
		}
		for (int i = 0; i < changedLineIndex; i++) {
			recipientLineItems.add(lineItemsInBillOrder.get(i));
		}
		return recipientLineItems;
	}

	private int findLineItemIndex(List<BillLineItem> lineItems, BillLineItem changedLineItem) {
		for (int i = 0; i < lineItems.size(); i++) {
			if (isSameLineItem(lineItems.get(i), changedLineItem)) {
				return i;
			}
		}
		return -1;
	}

	private boolean isSameLineItem(BillLineItem candidate, BillLineItem changedLineItem) {
		if (candidate == changedLineItem) {
			return true;
		}
		if (candidate == null || changedLineItem == null) {
			return false;
		}
		if (candidate.getId() != null && changedLineItem.getId() != null && candidate.getId().intValue() > 0
		        && candidate.getId().equals(changedLineItem.getId())) {
			return true;
		}
		return StringUtils.isNotBlank(candidate.getUuid()) && candidate.getUuid().equals(changedLineItem.getUuid());
	}

	private List<ReleasedPaymentChunk> trimChangedLineAllocations(BillLineItem changedLine, BigDecimal excess) {
		List<LinePaymentAllocation> allocations = getActiveCountableAllocations(changedLine);
		Collections.sort(allocations, new Comparator<LinePaymentAllocation>() {
			@Override
			public int compare(LinePaymentAllocation left, LinePaymentAllocation right) {
				return compareAllocationsNewestFirst(left, right);
			}
		});

		List<ReleasedPaymentChunk> releasedChunks = new ArrayList<ReleasedPaymentChunk>();
		BigDecimal remainingExcess = excess;
		Date dateVoided = new Date();
		User voidedBy = getAuthenticatedUser();
		for (LinePaymentAllocation allocation : allocations) {
			if (remainingExcess.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}

			BigDecimal allocationAmount = allocation.getAllocatedAmount();
			BigDecimal releasedAmount = allocationAmount.min(remainingExcess);
			releasedChunks.add(new ReleasedPaymentChunk(allocation.getPayment(), releasedAmount));
			if (allocationAmount.compareTo(releasedAmount) == 0) {
				setVoidProperties(allocation, "Rebalanced after line item allocation exceeded net total.", voidedBy,
				    dateVoided);
			} else {
				allocation.setAllocatedAmount(allocationAmount.subtract(releasedAmount));
			}
			remainingExcess = remainingExcess.subtract(releasedAmount);
		}
		return releasedChunks;
	}

	private List<LinePaymentAllocation> getActiveCountableAllocations(BillLineItem lineItem) {
		List<LinePaymentAllocation> allocations = new ArrayList<LinePaymentAllocation>();
		if (lineItem == null || lineItem.getAllocations() == null) {
			return allocations;
		}
		for (LinePaymentAllocation allocation : lineItem.getAllocations()) {
			if (allocation == null || Boolean.TRUE.equals(allocation.getVoided())
			        || allocation.getAllocatedAmount() == null
			        || allocation.getAllocatedAmount().compareTo(BigDecimal.ZERO) <= 0
			        || !isAllocatablePayment(allocation.getPayment())) {
				continue;
			}
			allocations.add(allocation);
		}
		return allocations;
	}

	private int compareAllocationsNewestFirst(LinePaymentAllocation left, LinePaymentAllocation right) {
		int dateComparison = compareDatesNewestFirst(left.getDateCreated(), right.getDateCreated());
		if (dateComparison != 0) {
			return dateComparison;
		}
		int idComparison = compareIntegersNewestFirst(left.getId(), right.getId());
		if (idComparison != 0) {
			return idComparison;
		}
		return compareStringsNewestFirst(left.getUuid(), right.getUuid());
	}

	private int compareDatesNewestFirst(Date left, Date right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return right.compareTo(left);
	}

	private int compareIntegersNewestFirst(Integer left, Integer right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return right.compareTo(left);
	}

	private int compareStringsNewestFirst(String left, String right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return right.compareTo(left);
	}

	private int compareIntegersAscending(Integer left, Integer right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return left.compareTo(right);
	}

	private int compareStringsAscending(String left, String right) {
		if (left == null && right == null) {
			return 0;
		}
		if (left == null) {
			return 1;
		}
		if (right == null) {
			return -1;
		}
		return left.compareTo(right);
	}

	private void reallocateReleasedChunks(Bill bill, List<BillLineItem> recipientLineItems,
	        List<ReleasedPaymentChunk> releasedChunks, Set<BillLineItem> affectedLineItems) {
		for (ReleasedPaymentChunk chunk : releasedChunks) {
			if (chunk == null || chunk.payment == null || chunk.remainingAmount.compareTo(BigDecimal.ZERO) <= 0
			        || !isAllocatablePayment(chunk.payment)) {
				continue;
			}
			for (BillLineItem recipient : recipientLineItems) {
				if (chunk.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
					break;
				}
				if (!isEligibleAllocationRecipient(recipient)) {
					continue;
				}

				BigDecimal remaining = recipient.getRemainingAmount();
				if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				BigDecimal allocationAmount = remaining.min(chunk.remainingAmount);
				LinePaymentAllocation allocation = createAllocation(bill, chunk.payment, recipient, allocationAmount);
				chunk.payment.addAllocation(allocation);
				recipient.addAllocation(allocation);
				affectedLineItems.add(recipient);
				chunk.remainingAmount = chunk.remainingAmount.subtract(allocationAmount);
			}
		}
	}

	private boolean isEligibleAllocationRecipient(BillLineItem lineItem) {
		if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
			return false;
		}
		return lineItem.getPaymentStatus() != BillStatus.EXEMPTED && lineItem.getPaymentStatus() != BillStatus.CANCELLED
		        && lineItem.getPaymentStatus() != BillStatus.ADJUSTED;
	}

	private LinePaymentAllocation createAllocation(Bill bill, Payment payment, BillLineItem lineItem,
	        BigDecimal allocationAmount) {
		LinePaymentAllocation allocation = new LinePaymentAllocation();
		allocation.setBill(bill);
		allocation.setPayment(payment);
		allocation.setBillLineItem(lineItem);
		allocation.setAllocatedAmount(allocationAmount);
		allocation.setCreator(resolveAllocationCreator(bill, payment, lineItem));
		allocation.setDateCreated(new Date());
		allocation.setVoided(false);
		allocation.setUuid(UUID.randomUUID().toString());
		return allocation;
	}

	private void synchronizeAffectedStatuses(Bill bill, Set<BillLineItem> affectedLineItems) {
		for (BillLineItem affectedLineItem : affectedLineItems) {
			if (affectedLineItem != null && !Boolean.TRUE.equals(affectedLineItem.getVoided())) {
				affectedLineItem.synchronizePaymentStatus();
			}
		}
		bill.synchronizeBillStatus();
	}

	private boolean isAllocatablePayment(Payment payment) {
		if (payment == null || Boolean.TRUE.equals(payment.getVoided())) {
			return false;
		}
		if (payment.getInstanceType() != null && payment.getInstanceType().getName() != null
		        && payment.getInstanceType().getName().equalsIgnoreCase("Waiver")) {
			return false;
		}
		return getPaymentAmountForAllocation(payment).compareTo(BigDecimal.ZERO) > 0;
	}

	private BigDecimal getPaymentAmountForAllocation(Payment payment) {
		if (payment == null) {
			return BigDecimal.ZERO;
		}
		if (payment.getAmountTendered() != null) {
			return payment.getAmountTendered();
		}
		if (payment.getAmount() != null) {
			return payment.getAmount();
		}
		return BigDecimal.ZERO;
	}

	private User resolveAllocationCreator(Bill bill, Payment payment, BillLineItem lineItem) {
		User authenticated = getAuthenticatedUser();
		if (authenticated != null) {
			return authenticated;
		}
		if (payment != null && payment.getCreator() != null) {
			return payment.getCreator();
		}
		if (lineItem != null && lineItem.getCreator() != null) {
			return lineItem.getCreator();
		}
		if (bill != null && bill.getCreator() != null) {
			return bill.getCreator();
		}
		return Context.getUserService().getUser(1);
	}

	protected User getAuthenticatedUser() {
		return Context.getAuthenticatedUser();
	}

	private void setVoidProperties(LinePaymentAllocation allocation, String reason, User user, Date dateVoided) {
		allocation.setVoided(true);
		allocation.setVoidReason(reason);
		allocation.setVoidedBy(user);
		allocation.setDateVoided(dateVoided);
	}

	private static class ReleasedPaymentChunk {
		private final Payment payment;
		private BigDecimal remainingAmount;

		private ReleasedPaymentChunk(Payment payment, BigDecimal remainingAmount) {
			this.payment = payment;
			this.remainingAmount = remainingAmount;
		}
	}
}
