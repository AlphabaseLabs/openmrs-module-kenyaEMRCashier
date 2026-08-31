/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.OpenmrsData;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.IReceiptNumberGenerator;
import org.openmrs.module.kenyaemr.cashier.api.ReceiptNumberGeneratorFactory;
import org.openmrs.module.kenyaemr.cashier.api.ITimesheetService;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.kenyaemr.cashier.api.base.f.Action1;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillingHistoryMetricsSummary;
import org.openmrs.module.kenyaemr.cashier.api.model.BillingHistorySummary;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.HistorySearchCriteria;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentHistoryMetricsSummary;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentHistorySummary;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMethodTotalSummary;
import org.openmrs.module.kenyaemr.cashier.api.model.Timesheet;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentAttributeService;
import org.openmrs.module.kenyaemr.cashier.api.base.exception.PrivilegeException;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.kenyaemr.cashier.api.util.PaymentReplayUtil;
import org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout.BrandingLogoProvider;
import org.openmrs.module.kenyaemr.cashier.util.Utils;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.security.AccessControlException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * Data service implementation class for {@link Bill}s.
 */
@Transactional
public class BillServiceImpl extends BaseEntityDataServiceImpl<Bill> implements IEntityAuthorizationPrivileges
        , IBillService {

	private static final int MAX_LENGTH_RECEIPT_NUMBER = 255;
	private static final int MAX_LENGTH_NOTE = 1024;
	private static final Log LOG = LogFactory.getLog(BillServiceImpl.class);
	private static final String GP_DEFAULT_LOCATION = "kenyaemr.defaultLocation";
	private static final String GP_FACILITY_ADDRESS_DETAILS = "kenyaemr.cashier.receipt.facilityAddress";
	private static final String GP_FACILITY_INFORMATION = "kenyaemr.cashier.receipt.facilityInformation";
	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static final String OPENMRS_ID = "05a29f94-c0ed-11e2-94be-8c13b969e334";
	public static final String PAYMENT_REFERENCE_ATTRIBUTE = "d453e528-0264-4d6e-ae23-bc0b777e1146";
	private static final String EMPTY_VALUE_DISPLAY = "--";
	private static final String CASH_PAYMENT_METHOD = "cash";
	private static final String WAIVER_PAYMENT_METHOD = "Waiver";
	private static final String REFERENCE_NUMBER_DESCRIPTION = "Reference Number";


	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}
	DecimalFormat df = new DecimalFormat("0.00");

	@Override
	protected void validate(Bill bill) {
		validateBillNote(bill);
		validatePaymentAttributes(bill);
	}

	private void validateBillNote(Bill bill) {
		if (bill != null && bill.getNote() != null && bill.getNote().length() > MAX_LENGTH_NOTE) {
			throw new IllegalArgumentException("The bill note must be 1024 characters or fewer.");
		}
	}

	@Override
	protected Collection<? extends OpenmrsObject> getRelatedObjects(Bill entity) {
		List<OpenmrsObject> related = new ArrayList<>();
		if (entity == null) {
			return related;
		}

		if (entity.getLineItems() != null) {
			for (BillLineItem lineItem : entity.getLineItems()) {
				if (lineItem == null) {
					continue;
				}
				related.add(lineItem);
				if (lineItem.getAdjustments() != null) {
					for (BillLineItemAdjustment adjustment : lineItem.getAdjustments()) {
						if (adjustment != null) {
							related.add(adjustment);
						}
					}
				}
			}
		}

		if (entity.getPayments() != null) {
			for (Payment payment : entity.getPayments()) {
				if (payment == null) {
					continue;
				}
				related.add(payment);
				if (payment.getAttributes() != null) {
					for (PaymentAttribute attribute : payment.getAttributes()) {
						if (attribute != null) {
							related.add(attribute);
						}
					}
				}
			}
		}

		return related;
	}

	/**
	 * Validates payment attributes to ensure no duplicate values exist within the same bill for the same attribute type.
	 * Only validates non-voided payments since voided payments are historical records.
	 * @param bill The bill to validate
	 */
	private void validatePaymentAttributes(Bill bill) {
		if (bill.getPayments() == null) {
			return;
		}

		// Track attribute values per attribute type across all NON-VOIDED payments in the bill
		Map<String, Set<String>> attributeTypeValues = new HashMap<>();
		
		for (Payment payment : bill.getPayments()) {
			// CRITICAL FIX: Skip voided payments - they are historical records and should not be validated
			// Voided payments may have attributes that conflict with new payments, but that's acceptable
			if (Boolean.TRUE.equals(payment.getVoided())) {
				continue;
			}
			
			if (payment.getAttributes() != null) {
				for (PaymentAttribute attribute : payment.getAttributes()) {
					String attributeTypeId = PaymentReplayUtil.getAttributeTypeKey(attribute);
					if (attributeTypeId != null && StringUtils.isNotBlank(attribute.getValue())) {
						String attributeValue = attribute.getValue().trim();
						
						// Initialize the set for this attribute type if it doesn't exist
						if (!attributeTypeValues.containsKey(attributeTypeId)) {
							attributeTypeValues.put(attributeTypeId, new HashSet<>());
						}
						
						// Check if this value already exists for this attribute type
						Set<String> existingValues = attributeTypeValues.get(attributeTypeId);
						if (existingValues.contains(attributeValue)) {
							throw new IllegalArgumentException(
								String.format("Duplicate payment attribute value '%s' found for attribute type '%s' across multiple payments in the same bill",
									attributeValue,
									attribute.getAttributeType().getName()));
						}
						
						// Add this value to the set
						existingValues.add(attributeValue);
					}
				}
			}
		}
	}

	private void normalizePaymentAttributeOwners(Payment payment) {
		if (payment == null || payment.getAttributes() == null) {
			return;
		}
		for (PaymentAttribute attribute : payment.getAttributes()) {
			if (attribute != null) {
				attribute.setOwner(payment);
			}
		}
	}

	private void normalizeLineItemPriceOverrides(Bill bill) {
		if (bill == null || bill.getLineItems() == null) {
			return;
		}
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem != null) {
				lineItem.normalizePriceOverride();
			}
		}
	}

	@Override
	@Transactional
	public Bill voidEntity(Bill bill, final String reason) {
		boolean canDeleteAnyBill = hasBillDeletePrivilege(PrivilegeConstants.FORCE_DELETE_BILLS);
		if (!canDeleteAnyBill && !hasBillDeletePrivilege(PrivilegeConstants.MANAGE_BILLS)) {
			throw new PrivilegeException();
		}

		if (bill == null) {
			throw new NullPointerException("The entity to void cannot be null.");
		}
		if (StringUtils.isEmpty(reason)) {
			throw new IllegalArgumentException("The reason to void must be defined.");
		}
		if (BillStatus.PAID.equals(bill.getStatus()) && !canDeleteAnyBill) {
			throw new AccessControlException("Access denied to delete a paid bill.");
		}

		final User user = getAuthenticatedUser();
		final Date dateVoided = new Date();
		setVoidProperties(bill, reason, user, dateVoided);

		List<OpenmrsData> updatedObjects = executeOnRelatedObjects(OpenmrsData.class, bill, new Action1<OpenmrsData>() {
			@Override
			public void apply(OpenmrsData data) {
				setVoidProperties(data, reason, user, dateVoided);
			}
		});

		validate(bill);
		if (!updatedObjects.isEmpty()) {
			Collection<OpenmrsObject> saveAll = new ArrayList<OpenmrsObject>();
			saveAll.add(bill);
			saveAll.addAll(updatedObjects);
			getRepository().saveAll(saveAll);
		} else {
			getRepository().save(bill);
		}

		return bill;
	}

	protected boolean hasBillDeletePrivilege(String privilege) {
		return Context.hasPrivilege(privilege);
	}

	protected User getAuthenticatedUser() {
		return Context.getAuthenticatedUser();
	}

	/**
	 * Determines whether an incoming payment should be merged into an existing open bill.
	 * This method is idempotency-aware: when a persisted payment already exists with the
	 * same identity (id/uuid) or same unique attribute value (e.g., Transaction Id),
	 * the incoming payment is treated as a replay and skipped.
	 */
	boolean shouldMergeIncomingPayment(Bill existingBill, Payment incomingPayment) {
		if (existingBill == null || incomingPayment == null || Boolean.TRUE.equals(incomingPayment.getVoided())) {
			return false;
		}
		if (existingBill.getPayments() == null || existingBill.getPayments().isEmpty()) {
			return true;
		}

		for (Payment existingPayment : existingBill.getPayments()) {
			if (PaymentReplayUtil.isReplayOf(existingPayment, incomingPayment)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Saves the bill to the database, creating a new bill or updating an existing one.
	 * @param bill The bill to be saved.
	 * @return The saved bill.
	 * @should Generate a new receipt number if one has not been defined.
	 * @should Not generate a receipt number if one has already been defined.
	 * @should Throw APIException if receipt number cannot be generated.
	 */
	@Override
	@Authorized({ PrivilegeConstants.MANAGE_BILLS })
	@Transactional
	public Bill save(Bill bill) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}

		/* Check for refund.
		 * A refund is given when the total of the bill's line items is negative.
		 */
		if (bill.getTotal().compareTo(BigDecimal.ZERO) < 0 && !Context.hasPrivilege(PrivilegeConstants.REFUND_MONEY)) {
			throw new AccessControlException("Access denied to give a refund.");
		}
		IReceiptNumberGenerator generator = ReceiptNumberGeneratorFactory.getGenerator();
		if (generator == null) {
			LOG.warn("No receipt number generator has been defined.  Bills will not be given a receipt number until one is"
			        + " defined.");
		} else {
			if (StringUtils.isEmpty(bill.getReceiptNumber())) {
				bill.setReceiptNumber(generator.generateNumber(bill));
			}
		}

		// If the bill has an ID, it's an update operation - save it directly
		if (bill.getId() != null) {
			LOG.info("Updating existing bill: " + bill.getReceiptNumber() + " with ID: " + bill.getId() + " and status: " + bill.getStatus());
			normalizeLineItemPriceOverrides(bill);
			allocatePaymentsToLineItems(bill);
			return super.save(bill);
		}

		List<Bill> bills = searchBill(bill.getPatient());
		if(!bills.isEmpty()) {
			Bill billToUpdate = bills.get(0);
			LOG.info("Found existing bill: " + billToUpdate.getReceiptNumber() + " with status: " + billToUpdate.getStatus() + ", closed: " + billToUpdate.isClosed() + ", voided: " + billToUpdate.getVoided());
			
			// Check if the existing bill is closed or voided
			if (billToUpdate.isClosed() || billToUpdate.getVoided()) {
				// If the bill is closed or voided, create a new bill instead of adding to the existing one
				LOG.info("Bill " + billToUpdate.getReceiptNumber() + " is closed or voided. Creating new bill for patient " + bill.getPatient().getPatientId());
				normalizeLineItemPriceOverrides(bill);
				allocatePaymentsToLineItems(bill);
				return super.save(bill);
			}
			
			// If the existing bill is not closed, add new items to it
			// Set status to PENDING if it was PAID/POSTED to allow new items
			if (billToUpdate.getStatus() == BillStatus.PAID || billToUpdate.getStatus() == BillStatus.POSTED) {
				LOG.info("Setting bill status from " + billToUpdate.getStatus() + " to PENDING to allow new items");
				billToUpdate.setStatus(BillStatus.PENDING);
			}
			
			// Create a copy of the line items to avoid ConcurrentModificationException
			List<BillLineItem> itemsToAdd = new ArrayList<>(bill.getLineItems());
			for (BillLineItem item: itemsToAdd) {
				item.setBill(billToUpdate);
				billToUpdate.getLineItems().add(item);
			}

			// Merge incoming payments as well; previously these were ignored for existing open bills.
			if (bill.getPayments() != null && !bill.getPayments().isEmpty()) {
				boolean mergedPayments = false;
				for (Payment payment : new ArrayList<>(bill.getPayments())) {
					if (payment == null) {
						continue;
					}
					if (!shouldMergeIncomingPayment(billToUpdate, payment)) {
						LOG.info("Skipping duplicate incoming payment for bill " + billToUpdate.getReceiptNumber()
						        + " (paymentId=" + payment.getId() + ", paymentUuid=" + payment.getUuid() + ")");
						continue;
					}
					normalizePaymentAttributeOwners(payment);
					billToUpdate.addPayment(payment);
					mergedPayments = true;
				}
				if (!mergedPayments) {
					// Keep status in sync when all incoming payments are idempotent replays.
					billToUpdate.synchronizeBillStatus();
				}
			} else {
				// Keep status in sync for item-only updates.
				billToUpdate.synchronizeBillStatus();
			}
			// appending items to existing non-closed bill
			LOG.info("Adding " + itemsToAdd.size() + " items to existing bill: " + billToUpdate.getReceiptNumber());
			normalizeLineItemPriceOverrides(billToUpdate);
			allocatePaymentsToLineItems(billToUpdate);
			return super.save(billToUpdate);
		} else {
			LOG.info("No existing bills found for patient " + bill.getPatient().getPatientId() + ", creating new bill");
		}

		normalizeLineItemPriceOverrides(bill);
		allocatePaymentsToLineItems(bill);
		return super.save(bill);
	}

	private void allocatePaymentsToLineItems(Bill bill) {
		if (bill == null || bill.getPayments() == null || bill.getLineItems() == null) {
			return;
		}

		List<BillLineItem> lineItemsInPayloadOrder = new ArrayList<BillLineItem>();
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem != null) {
				lineItemsInPayloadOrder.add(lineItem);
			}
		}
		if (lineItemsInPayloadOrder.isEmpty()) {
			return;
		}

		for (Payment payment : bill.getPayments()) {
			if (!isAllocatablePayment(payment)) {
				continue;
			}

			payment.setBill(bill);
			BigDecimal unallocated = getPaymentAmountForAllocation(payment).subtract(payment.getTotalAllocated());
			if (unallocated.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}

			for (BillLineItem lineItem : lineItemsInPayloadOrder) {
				if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
					continue;
				}
				if (lineItem.getPaymentStatus() == BillStatus.EXEMPTED || lineItem.getPaymentStatus() == BillStatus.CANCELLED
				        || lineItem.getPaymentStatus() == BillStatus.ADJUSTED) {
					continue;
				}

				BigDecimal remaining = lineItem.getRemainingAmount();
				if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				BigDecimal allocationAmount = remaining.min(unallocated);
				if (allocationAmount.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				LinePaymentAllocation allocation = new LinePaymentAllocation();
				allocation.setBill(bill);
				allocation.setPayment(payment);
				allocation.setBillLineItem(lineItem);
				allocation.setAllocatedAmount(allocationAmount);
				allocation.setCreator(resolveAllocationCreator(bill, payment, lineItem));
				allocation.setDateCreated(new Date());
				allocation.setVoided(false);
				allocation.setUuid(UUID.randomUUID().toString());

				payment.addAllocation(allocation);
				lineItem.addAllocation(allocation);

				unallocated = unallocated.subtract(allocationAmount);
				if (unallocated.compareTo(BigDecimal.ZERO) <= 0) {
					break;
				}
			}
		}

		// Update each line item's paymentStatus after allocations are created so the
		// status reflects both allocations and zero-balance discount cases.
		for (BillLineItem lineItem : lineItemsInPayloadOrder) {
			if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
				continue;
			}
			lineItem.synchronizePaymentStatus();
		}
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

	private User resolveAllocationCreator(Bill bill, Payment payment, BillLineItem lineItem) {
		User authenticated = Context.getAuthenticatedUser();
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

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	@Transactional(readOnly = true)
	public Bill getBillByReceiptNumber(String receiptNumber) {
		if (StringUtils.isEmpty(receiptNumber)) {
			throw new IllegalArgumentException("The receipt number must be defined.");
		}
		if (receiptNumber.length() > MAX_LENGTH_RECEIPT_NUMBER) {
			throw new IllegalArgumentException("The receipt number must be less than 256 characters.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("receiptNumber", receiptNumber));

		Bill bill = getRepository().selectSingle(getEntityClass(), criteria);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> getBillsByPatient(Patient patient, PagingInfo paging) {
		if (patient == null) {
			throw new NullPointerException("The patient must be defined.");
		}

		return getBillsByPatientId(patient.getId(), paging);
	}

	@Override
	public List<Bill> getBillsByPatientId(int patientId, PagingInfo paging) {
		if (patientId < 0) {
			throw new IllegalArgumentException("The patient id must be a valid identifier.");
		}

		Criteria criteria = getRepository().createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("patient.id", patientId));
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(getEntityClass(), createPagingCriteria(paging, criteria));
		removeNullLineItems(results);

		return results;
	}

	@Override
	public List<Bill> getBills(final BillSearch billSearch) {
		return getBills(billSearch, null);
	}

	@Override
	public List<Bill> getBills(final BillSearch billSearch, PagingInfo pagingInfo) {
		if (billSearch == null) {
			throw new NullPointerException("The bill search must be defined.");
		} else if (billSearch.getTemplate() == null) {
			throw new NullPointerException("The bill search template must be defined.");
		}

		List<Bill> results = executeCriteria(Bill.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billSearch.updateCriteria(criteria);
			}
		}, Order.desc("id"));
		
		// Clean up null line items before returning
		removeNullLineItems(results);
		return results;
	}

	/*
		These methods are overridden to ensure that any null line items (created as part of a bug in 1.7.0) are removed
		from the results before being returned to the caller.
	 */
	@Override
	public List<Bill> getAll(boolean includeVoided, PagingInfo pagingInfo) {
		List<Bill> results = super.getAll(includeVoided, pagingInfo);
		removeNullLineItems(results);
		return results;
	}

	@Override
	public Bill getById(int entityId) {
		Bill bill = super.getById(entityId);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public Bill getByUuid(String uuid) {
		Bill bill = super.getByUuid(uuid);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> getAll() {
		List<Bill> results = super.getAll();
		removeNullLineItems(results);
		return results;
	}

	private void removeNullLineItems(List<Bill> bills) {
		if (bills == null || bills.size() == 0) {
			return;
		}

		for (Bill bill : bills) {
			removeNullLineItems(bill);
		}
	}

	private void removeNullLineItems(Bill bill) {
		if (bill == null) {
			return;
		}

		// Search for any null line items (due to a bug in 1.7.0) and remove them from the line items
		int index = bill.getLineItems().indexOf(null);
		while (index >= 0) {
			bill.getLineItems().remove(index);

			index = bill.getLineItems().indexOf(null);
		}
		// Note: We don't remove voided line items here to avoid conflicts with REST API filtering
		// The REST layer will handle voided item filtering based on the includeVoidedLineItems parameter
	}

	@Override
	public String getVoidPrivilege() {
		return PrivilegeConstants.MANAGE_BILLS;
	}

	@Override
	public String getSavePrivilege() {
		return PrivilegeConstants.MANAGE_BILLS;
	}

	@Override
	public String getPurgePrivilege() {
		return PrivilegeConstants.PURGE_BILLS;
	}

	@Override
	public String getGetPrivilege() {
		return PrivilegeConstants.VIEW_BILLS;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<Bill> searchBill(Patient patient) {
		Criteria criteria = getRepository().createCriteria(Bill.class);

		// Look for any non-closed bills for the same patient, regardless of date
		// This ensures that bills spanning multiple days remain as one bill
		// until explicitly closed
		// Also treat voided bills as closed bills
		criteria.add(Restrictions.eq("patient", patient));
		criteria.add(Restrictions.eq("closed", false)); // Exclude closed bills
		criteria.add(Restrictions.eq("voided", false)); // Exclude voided bills (treat as closed)
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(Bill.class, criteria);
		removeNullLineItems(results);
		return results;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<Bill> getAllBillsForPatient(Patient patient) {
		Criteria criteria = getRepository().createCriteria(Bill.class);

		// Look for all bills for the same patient, including closed ones
		criteria.add(Restrictions.eq("patient", patient));
		criteria.addOrder(Order.desc("id"));

		List<Bill> results = getRepository().select(Bill.class, criteria);
		removeNullLineItems(results);
		return results;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<BillingHistorySummary> getBillingHistory(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		if (resolvedCriteria.emptyResult) {
			return new ArrayList<BillingHistorySummary>();
		}

		List<Integer> billIds = findBillingHistoryBillIds(resolvedCriteria, getCriteriaStartIndex(criteria),
		    getCriteriaLimit(criteria));
		List<Bill> bills = loadBillsByIds(billIds);
		List<BillingHistorySummary> summaries = new ArrayList<BillingHistorySummary>(bills.size());
		for (Bill bill : bills) {
			summaries.add(toBillingHistorySummary(bill, resolvedCriteria));
		}
		return summaries;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public long getBillingHistoryCount(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		return resolvedCriteria.emptyResult ? 0 : countBillingHistoryBills(resolvedCriteria);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public BillingHistorySummary getBillingHistoryByUuid(String billUuid, HistorySearchCriteria criteria) {
		Bill bill = getByUuid(billUuid);
		return bill == null ? null : toBillingHistorySummary(bill, resolveHistoryCriteria(criteria));
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public BillingHistoryMetricsSummary getBillingHistoryMetrics(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		if (resolvedCriteria.emptyResult) {
			return new BillingHistoryMetricsSummary();
		}

		List<Bill> bills = loadBillsByIds(findBillingHistoryBillIds(resolvedCriteria, null, null));
		BillingHistoryMetricsSummary metrics = new BillingHistoryMetricsSummary();
		Map<String, BigDecimal> paymentModeTotals = new HashMap<String, BigDecimal>();
		BigDecimal totalBills = BigDecimal.ZERO;
		BigDecimal totalPayments = BigDecimal.ZERO;
		BigDecimal totalDue = BigDecimal.ZERO;
		BigDecimal totalDiscount = BigDecimal.ZERO;
		BigDecimal waivedAmount = BigDecimal.ZERO;
		BigDecimal exemptedAmount = BigDecimal.ZERO;
		BigDecimal taxCollectionAmount = BigDecimal.ZERO;

		for (Bill bill : bills) {
			List<Payment> visiblePayments = getVisiblePayments(bill, resolvedCriteria);
			BigDecimal billAmount = safeBigDecimal(bill.getTotal());
			BigDecimal paidAmount = getActualPaymentTotal(visiblePayments);
			totalBills = totalBills.add(billAmount);
			totalPayments = totalPayments.add(paidAmount);
			totalDue = totalDue.add(calculateMetricsDueAmount(bill, paidAmount));
			totalDiscount = totalDiscount.add(safeBigDecimal(bill.getTotalDiscount()));
			waivedAmount = waivedAmount.add(safeBigDecimal(bill.getTotalWaivers()));
			exemptedAmount = exemptedAmount.add(safeBigDecimal(bill.getTotalExempted()));

			if (BillStatus.PAID.equals(bill.getStatus())) {
				taxCollectionAmount = taxCollectionAmount.add(safeBigDecimal(bill.getTotalTax()));
			}

			addPaymentMethodTotals(paymentModeTotals, visiblePayments);
		}

		metrics.setTotalBills(totalBills);
		metrics.setTotalPayments(totalPayments);
		metrics.setTotalDue(totalDue);
		metrics.setTotalDiscount(totalDiscount);
		metrics.setWaivedAmount(waivedAmount);
		metrics.setExemptedAmount(exemptedAmount);
		metrics.setTaxCollectionAmount(taxCollectionAmount);
		metrics.setPaymentMethodTotals(toPaymentMethodTotals(paymentModeTotals));
		return metrics;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public List<PaymentHistorySummary> getPaymentHistory(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		if (resolvedCriteria.emptyResult) {
			return new ArrayList<PaymentHistorySummary>();
		}

		List<Integer> paymentIds = findPaymentHistoryPaymentIds(resolvedCriteria, getCriteriaStartIndex(criteria),
		    getCriteriaLimit(criteria));
		List<Payment> payments = loadPaymentsByIds(paymentIds);
		List<PaymentHistorySummary> summaries = new ArrayList<PaymentHistorySummary>(payments.size());
		for (Payment payment : payments) {
			summaries.add(toPaymentHistorySummary(payment));
		}
		return summaries;
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public long getPaymentHistoryCount(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		return resolvedCriteria.emptyResult ? 0 : countPaymentHistoryPayments(resolvedCriteria);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public PaymentHistorySummary getPaymentHistoryByUuid(String paymentUuid, HistorySearchCriteria criteria) {
		Payment payment = findPaymentByUuid(paymentUuid);
		return payment == null ? null : toPaymentHistorySummary(payment);
	}

	@Override
	@Authorized({ PrivilegeConstants.VIEW_BILLS })
	public PaymentHistoryMetricsSummary getPaymentHistoryMetrics(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolvedCriteria = resolveHistoryCriteria(criteria);
		if (resolvedCriteria.emptyResult) {
			return new PaymentHistoryMetricsSummary();
		}

		List<Payment> payments = loadPaymentsByIds(findPaymentHistoryPaymentIds(resolvedCriteria, null, null));
		PaymentHistoryMetricsSummary metrics = new PaymentHistoryMetricsSummary();
		Map<String, BigDecimal> paymentModeTotals = new HashMap<String, BigDecimal>();
		Map<String, BigDecimal> payeeTotals = new HashMap<String, BigDecimal>();
		Map<String, String> payeeNames = new HashMap<String, String>();
		BigDecimal totalPayments = BigDecimal.ZERO;
		BigDecimal cash = BigDecimal.ZERO;
		BigDecimal others = BigDecimal.ZERO;

		for (Payment payment : payments) {
			BigDecimal amount = safeBigDecimal(payment.getAmountTendered());
			String paymentMethod = normalizeValue(getPaymentMethodName(payment));
			String payeeKey = buildPayeeKey(payment.getBill());
			String patientName = getPatientName(payment.getBill());

			totalPayments = totalPayments.add(amount);
			if (CASH_PAYMENT_METHOD.equalsIgnoreCase(paymentMethod)) {
				cash = cash.add(amount);
			} else {
				others = others.add(amount);
			}

			addAmount(paymentModeTotals, paymentMethod, amount);
			addAmount(payeeTotals, payeeKey, amount);
			payeeNames.put(payeeKey, patientName);
		}

		metrics.setTotalPayments(totalPayments);
		metrics.setCash(cash);
		metrics.setOthers(others);
		PayeeSummary topPayee = resolveTopPayee(payeeTotals, payeeNames);
		metrics.setTopPayeeName(topPayee.name);
		metrics.setTopPayeeAmount(topPayee.amount);
		metrics.setPaymentMethodTotals(toPaymentMethodTotals(paymentModeTotals));
		return metrics;
	}

	private ResolvedHistoryCriteria resolveHistoryCriteria(HistorySearchCriteria criteria) {
		ResolvedHistoryCriteria resolved = new ResolvedHistoryCriteria();
		if (criteria == null) {
			return resolved;
		}

		resolved.fromDate = criteria.getFromDate();
		resolved.toDate = criteria.getToDate();
		resolved.patientUuid = normalizeFilterValue(criteria.getPatientUuid());
		resolved.status = criteria.getStatus();
		resolved.paymentModes = normalizeFilterValues(criteria.getPaymentModes());
		resolved.cashierUuids = normalizeFilterValues(criteria.getCashierUuids());

		String timesheetUuid = normalizeFilterValue(criteria.getTimesheetUuid());
		if (timesheetUuid == null) {
			return resolved;
		}

		Timesheet timesheet = Context.getService(ITimesheetService.class).getByUuid(timesheetUuid);
		if (timesheet == null || Boolean.TRUE.equals(timesheet.getVoided()) || timesheet.getCashier() == null) {
			resolved.emptyResult = true;
			return resolved;
		}

		String timesheetCashierUuid = timesheet.getCashier().getUuid();
		if (!resolved.cashierUuids.isEmpty() && !resolved.cashierUuids.contains(timesheetCashierUuid)) {
			resolved.emptyResult = true;
			return resolved;
		}

		resolved.cashierUuids = new ArrayList<String>();
		resolved.cashierUuids.add(timesheetCashierUuid);
		resolved.fromDate = maxDate(resolved.fromDate, timesheet.getClockIn());
		resolved.toDate = minDate(resolved.toDate, timesheet.getClockOut() == null ? new Date() : timesheet.getClockOut());
		if (resolved.fromDate != null && resolved.toDate != null && resolved.fromDate.after(resolved.toDate)) {
			resolved.emptyResult = true;
		}

		return resolved;
	}

	private List<Integer> findBillingHistoryBillIds(ResolvedHistoryCriteria criteria, Integer startIndex, Integer limit) {
		StringBuilder hql = buildBillingHistoryQuery(criteria, false);
		hql.append(" order by b.dateCreated desc, b.id desc");

		Query query = getRepository().createQuery(hql.toString());
		applyHistoryQueryParameters(query, criteria);
		applyOffsetLimit(query, startIndex, limit);
		return query.list();
	}

	private long countBillingHistoryBills(ResolvedHistoryCriteria criteria) {
		Query query = getRepository().createQuery(buildBillingHistoryQuery(criteria, true).toString());
		applyHistoryQueryParameters(query, criteria);
		Long count = (Long) query.uniqueResult();
		return count == null ? 0 : count.longValue();
	}

	private List<Integer> findPaymentHistoryPaymentIds(ResolvedHistoryCriteria criteria, Integer startIndex, Integer limit) {
		StringBuilder hql = buildPaymentHistoryQuery(criteria, false);
		hql.append(" order by p.dateCreated desc, p.id desc");

		Query query = getRepository().createQuery(hql.toString());
		applyHistoryQueryParameters(query, criteria);
		applyOffsetLimit(query, startIndex, limit);
		return query.list();
	}

	private long countPaymentHistoryPayments(ResolvedHistoryCriteria criteria) {
		Query query = getRepository().createQuery(buildPaymentHistoryQuery(criteria, true).toString());
		applyHistoryQueryParameters(query, criteria);
		Long count = (Long) query.uniqueResult();
		return count == null ? 0 : count.longValue();
	}

	private StringBuilder buildBillingHistoryQuery(ResolvedHistoryCriteria criteria, boolean countOnly) {
		StringBuilder hql = new StringBuilder(countOnly ? "select count(distinct b.id) from Bill b" : "select distinct b.id from Bill b");
		if (!criteria.paymentModes.isEmpty()) {
			hql.append(" join b.payments p");
		}
		hql.append(" where b.voided = false");
		appendBillingHistoryFilters(hql, criteria);
		return hql;
	}

	private void appendBillingHistoryFilters(StringBuilder hql, ResolvedHistoryCriteria criteria) {
		if (criteria.fromDate != null) {
			hql.append(" and b.dateCreated >= :fromDate");
		}
		if (criteria.toDate != null) {
			hql.append(" and b.dateCreated <= :toDate");
		}
		if (criteria.patientUuid != null) {
			hql.append(" and b.patient.uuid = :patientUuid");
		}
		if (criteria.status != null) {
			hql.append(" and b.status = :status");
		}
		if (!criteria.cashierUuids.isEmpty()) {
			hql.append(" and b.cashier.uuid in (:cashierUuids)");
		}
		if (!criteria.paymentModes.isEmpty()) {
			hql.append(" and p.voided = false and p.instanceType.name in (:paymentModes)");
		}
	}

	private StringBuilder buildPaymentHistoryQuery(ResolvedHistoryCriteria criteria, boolean countOnly) {
		StringBuilder hql = new StringBuilder(countOnly ? "select count(p.id) from Payment p join p.bill b"
		        : "select p.id from Payment p join p.bill b");
		hql.append(" where p.voided = false and b.voided = false");
		appendPaymentHistoryFilters(hql, criteria);
		return hql;
	}

	private void appendPaymentHistoryFilters(StringBuilder hql, ResolvedHistoryCriteria criteria) {
		if (criteria.fromDate != null) {
			hql.append(" and p.dateCreated >= :fromDate");
		}
		if (criteria.toDate != null) {
			hql.append(" and p.dateCreated <= :toDate");
		}
		if (criteria.patientUuid != null) {
			hql.append(" and b.patient.uuid = :patientUuid");
		}
		if (criteria.status != null) {
			hql.append(" and b.status = :status");
		}
		if (!criteria.cashierUuids.isEmpty()) {
			hql.append(" and b.cashier.uuid in (:cashierUuids)");
		}
		if (!criteria.paymentModes.isEmpty()) {
			hql.append(" and p.instanceType.name in (:paymentModes)");
		}
	}

	private void applyHistoryQueryParameters(Query query, ResolvedHistoryCriteria criteria) {
		if (criteria.fromDate != null) {
			query.setTimestamp("fromDate", criteria.fromDate);
		}
		if (criteria.toDate != null) {
			query.setTimestamp("toDate", criteria.toDate);
		}
		if (criteria.patientUuid != null) {
			query.setString("patientUuid", criteria.patientUuid);
		}
		if (criteria.status != null) {
			query.setParameter("status", criteria.status);
		}
		if (!criteria.cashierUuids.isEmpty()) {
			query.setParameterList("cashierUuids", criteria.cashierUuids);
		}
		if (!criteria.paymentModes.isEmpty()) {
			query.setParameterList("paymentModes", criteria.paymentModes);
		}
	}

	private void applyOffsetLimit(Query query, Integer startIndex, Integer limit) {
		if (startIndex != null && startIndex.intValue() >= 0) {
			query.setFirstResult(startIndex.intValue());
		}
		if (limit != null && limit.intValue() > 0) {
			query.setMaxResults(limit.intValue());
			query.setFetchSize(limit.intValue());
		}
	}

	private List<Bill> loadBillsByIds(List<Integer> billIds) {
		if (billIds == null || billIds.isEmpty()) {
			return new ArrayList<Bill>();
		}

		Criteria criteria = getRepository().createCriteria(Bill.class);
		criteria.add(Restrictions.in("id", billIds));
		List<Bill> bills = getRepository().select(Bill.class, criteria);
		removeNullLineItems(bills);
		return orderEntitiesByIds(billIds, bills);
	}

	private List<Payment> loadPaymentsByIds(List<Integer> paymentIds) {
		if (paymentIds == null || paymentIds.isEmpty()) {
			return new ArrayList<Payment>();
		}

		Criteria criteria = getRepository().createCriteria(Payment.class);
		criteria.add(Restrictions.in("id", paymentIds));
		List<Payment> payments = getRepository().select(Payment.class, criteria);
		return orderEntitiesByIds(paymentIds, payments);
	}

	private <T extends OpenmrsObject> List<T> orderEntitiesByIds(List<Integer> ids, List<T> entities) {
		Map<Integer, T> entityById = new HashMap<Integer, T>();
		for (T entity : entities) {
			entityById.put(entity.getId(), entity);
		}

		List<T> orderedEntities = new ArrayList<T>(ids.size());
		for (Integer id : ids) {
			T entity = entityById.get(id);
			if (entity != null) {
				orderedEntities.add(entity);
			}
		}
		return orderedEntities;
	}

	private Payment findPaymentByUuid(String paymentUuid) {
		if (StringUtils.isBlank(paymentUuid)) {
			return null;
		}

		Criteria criteria = getRepository().createCriteria(Payment.class);
		criteria.add(Restrictions.eq("uuid", paymentUuid));
		return getRepository().selectSingle(Payment.class, criteria);
	}

	private BillingHistorySummary toBillingHistorySummary(Bill bill, ResolvedHistoryCriteria criteria) {
		BillingHistorySummary summary = new BillingHistorySummary();
		List<Payment> visiblePayments = getVisiblePayments(bill, criteria);
		summary.setUuid(bill.getUuid());
		summary.setBillId(bill.getId());
		summary.setReceiptNumber(bill.getReceiptNumber());
		summary.setPatientUuid(bill.getPatient() == null ? null : bill.getPatient().getUuid());
		summary.setPatientName(getPatientName(bill));
		summary.setIdentifier(getPatientIdentifier(bill));
		summary.setDateCreated(bill.getDateCreated());
		summary.setStatus(bill.getStatus() == null ? null : bill.getStatus().name());
		summary.setTotalAmount(safeBigDecimal(bill.getTotal()));
		summary.setTotalDiscount(safeBigDecimal(bill.getTotalDiscount()));
		summary.setTotalPaid(getActualPaymentTotal(visiblePayments));
		summary.setAmountDue(safeBigDecimal(bill.getBalance()));
		summary.setBilledItems(buildBilledItemsDisplay(bill));
		summary.setReferenceCodes(buildReferenceCodes(visiblePayments));
		return summary;
	}

	private PaymentHistorySummary toPaymentHistorySummary(Payment payment) {
		Bill bill = payment.getBill();
		PaymentHistorySummary summary = new PaymentHistorySummary();
		summary.setUuid(payment.getUuid());
		summary.setPaymentId(payment.getId());
		summary.setBillUuid(bill == null ? null : bill.getUuid());
		summary.setPatientUuid(bill == null || bill.getPatient() == null ? null : bill.getPatient().getUuid());
		summary.setPatientName(getPatientName(bill));
		summary.setIdentifier(getPatientIdentifier(bill));
		summary.setInvoiceId(bill == null ? EMPTY_VALUE_DISPLAY : normalizeValue(bill.getReceiptNumber()));
		summary.setPaymentDate(payment.getDateCreated());
		summary.setPaymentAmount(safeBigDecimal(payment.getAmountTendered()));
		summary.setPaymentMethod(normalizeValue(getPaymentMethodName(payment)));
		summary.setReferenceId(buildPaymentReferenceId(payment));
		return summary;
	}

	private List<Payment> getVisiblePayments(Bill bill, ResolvedHistoryCriteria criteria) {
		List<Payment> visiblePayments = new ArrayList<Payment>();
		if (bill == null || bill.getPayments() == null) {
			return visiblePayments;
		}

		for (Payment payment : bill.getPayments()) {
			if (payment == null || Boolean.TRUE.equals(payment.getVoided())) {
				continue;
			}
			if (!criteria.paymentModes.isEmpty() && !criteria.paymentModes.contains(getPaymentMethodName(payment))) {
				continue;
			}
			visiblePayments.add(payment);
		}

		return visiblePayments;
	}

	private BigDecimal getActualPaymentTotal(List<Payment> payments) {
		BigDecimal total = BigDecimal.ZERO;
		for (Payment payment : payments) {
			if (payment == null || Boolean.TRUE.equals(payment.getVoided())) {
				continue;
			}
			if (WAIVER_PAYMENT_METHOD.equalsIgnoreCase(getPaymentMethodName(payment))) {
				continue;
			}
			total = total.add(safeBigDecimal(payment.getAmountTendered()));
		}
		return total;
	}

	private BigDecimal calculateMetricsDueAmount(Bill bill, BigDecimal visibleActualPayments) {
		BigDecimal due = safeBigDecimal(bill.getTotal()).subtract(visibleActualPayments)
		        .subtract(safeBigDecimal(bill.getTotalWaivers()));
		return due.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : due;
	}

	private void addPaymentMethodTotals(Map<String, BigDecimal> paymentModeTotals, List<Payment> payments) {
		for (Payment payment : payments) {
			String paymentMethod = normalizeValue(getPaymentMethodName(payment));
			addAmount(paymentModeTotals, paymentMethod, safeBigDecimal(payment.getAmountTendered()));
		}
	}

	private void addAmount(Map<String, BigDecimal> totals, String key, BigDecimal amount) {
		totals.put(key, safeBigDecimal(totals.get(key)).add(safeBigDecimal(amount)));
	}

	private PayeeSummary resolveTopPayee(Map<String, BigDecimal> payeeTotals, Map<String, String> payeeNames) {
		String topPayeeName = null;
		BigDecimal topPayeeAmount = BigDecimal.ZERO;

		for (Map.Entry<String, BigDecimal> entry : payeeTotals.entrySet()) {
			BigDecimal amount = entry.getValue();
			String name = payeeNames.get(entry.getKey());
			if (amount.compareTo(topPayeeAmount) > 0
			        || amount.compareTo(topPayeeAmount) == 0 && topPayeeName != null && name != null
			                && name.compareToIgnoreCase(topPayeeName) < 0) {
				topPayeeAmount = amount;
				topPayeeName = name;
			}
			if (topPayeeName == null && name != null) {
				topPayeeName = name;
			}
		}

		return new PayeeSummary(topPayeeName, topPayeeAmount);
	}

	private List<PaymentMethodTotalSummary> toPaymentMethodTotals(Map<String, BigDecimal> paymentModeTotals) {
		List<PaymentMethodTotalSummary> totals = new ArrayList<PaymentMethodTotalSummary>(paymentModeTotals.size());
		for (Map.Entry<String, BigDecimal> entry : paymentModeTotals.entrySet()) {
			totals.add(new PaymentMethodTotalSummary(entry.getKey(), entry.getValue()));
		}
		Collections.sort(totals, new Comparator<PaymentMethodTotalSummary>() {
			@Override
			public int compare(PaymentMethodTotalSummary first, PaymentMethodTotalSummary second) {
				int amountComparison = second.getTotal().compareTo(first.getTotal());
				if (amountComparison != 0) {
					return amountComparison;
				}
				return first.getPaymentMethod().compareToIgnoreCase(second.getPaymentMethod());
			}
		});
		return totals;
	}

	private String buildBilledItemsDisplay(Bill bill) {
		if (bill == null || bill.getLineItems() == null || bill.getLineItems().isEmpty()) {
			return EMPTY_VALUE_DISPLAY;
		}

		List<String> names = new ArrayList<String>();
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
				continue;
			}
			names.add(extractServiceName(lineItem));
		}
		return names.isEmpty() ? EMPTY_VALUE_DISPLAY : StringUtils.join(names, ", ");
	}

	private String buildReferenceCodes(List<Payment> payments) {
		List<String> codes = new ArrayList<String>();
		for (Payment payment : payments) {
			String referenceValue = buildPaymentReferenceValue(payment);
			if (StringUtils.isNotBlank(referenceValue)) {
				codes.add(getPaymentMethodName(payment) + ": " + referenceValue);
			}
		}
		return codes.isEmpty() ? EMPTY_VALUE_DISPLAY : StringUtils.join(codes, ", ");
	}

	private String buildPaymentReferenceId(Payment payment) {
		List<String> referenceIds = getPaymentAttributeValues(payment, true);
		if (!referenceIds.isEmpty()) {
			return StringUtils.join(referenceIds, ", ");
		}

		List<String> fallbackValues = getPaymentAttributeValues(payment, false);
		return fallbackValues.isEmpty() ? EMPTY_VALUE_DISPLAY : StringUtils.join(fallbackValues, ", ");
	}

	private String buildPaymentReferenceValue(Payment payment) {
		List<String> values = getPaymentAttributeValues(payment, false);
		return values.isEmpty() ? null : StringUtils.join(values, ", ");
	}

	private List<String> getPaymentAttributeValues(Payment payment, boolean referenceOnly) {
		List<String> values = new ArrayList<String>();
		if (payment == null || payment.getAttributes() == null) {
			return values;
		}

		for (PaymentAttribute attribute : payment.getAttributes()) {
			if (attribute == null || StringUtils.isBlank(attribute.getValue())) {
				continue;
			}
			if (referenceOnly && (attribute.getAttributeType() == null
			        || !REFERENCE_NUMBER_DESCRIPTION.equals(attribute.getAttributeType().getDescription()))) {
				continue;
			}
			values.add(attribute.getValue().trim());
		}
		return values;
	}

	private String getPaymentMethodName(Payment payment) {
		return payment == null || payment.getInstanceType() == null || StringUtils.isBlank(payment.getInstanceType().getName())
		        ? EMPTY_VALUE_DISPLAY
		        : payment.getInstanceType().getName();
	}

	private String getPatientName(Bill bill) {
		if (bill == null || bill.getPatient() == null || bill.getPatient().getPersonName() == null) {
			return EMPTY_VALUE_DISPLAY;
		}
		return normalizeValue(bill.getPatient().getPersonName().getFullName());
	}

	private String getPatientIdentifier(Bill bill) {
		return bill == null || bill.getPatient() == null || bill.getPatient().getPatientIdentifier() == null
		        || StringUtils.isBlank(bill.getPatient().getPatientIdentifier().getIdentifier()) ? EMPTY_VALUE_DISPLAY
		                : bill.getPatient().getPatientIdentifier().getIdentifier();
	}

	private String buildPayeeKey(Bill bill) {
		if (bill == null || bill.getPatient() == null) {
			return EMPTY_VALUE_DISPLAY;
		}
		return StringUtils.isBlank(bill.getPatient().getUuid()) ? getPatientName(bill) : bill.getPatient().getUuid();
	}

	private String extractServiceName(BillLineItem lineItem) {
		String rawName = null;
		if (lineItem.getBillableService() != null && StringUtils.isNotBlank(lineItem.getBillableService().getName())) {
			rawName = lineItem.getBillableService().getName();
		} else if (lineItem.getItem() != null && StringUtils.isNotBlank(lineItem.getItem().getCommonName())) {
			rawName = lineItem.getItem().getCommonName();
		}

		if (StringUtils.isBlank(rawName)) {
			return EMPTY_VALUE_DISPLAY;
		}

		String[] parts = rawName.split(":");
		if (parts.length == 1) {
			return rawName.trim();
		}

		return parts[0].trim().matches("^[0-9a-fA-F-]{36}$") ? parts[1].trim() : parts[0].trim();
	}

	private String normalizeValue(String value) {
		return StringUtils.isBlank(value) ? EMPTY_VALUE_DISPLAY : value.trim();
	}

	private String normalizeFilterValue(String value) {
		return StringUtils.isBlank(value) ? null : value.trim();
	}

	private Integer getCriteriaLimit(HistorySearchCriteria criteria) {
		return criteria == null ? null : criteria.getLimit();
	}

	private Integer getCriteriaStartIndex(HistorySearchCriteria criteria) {
		return criteria == null ? null : criteria.getStartIndex();
	}

	private BigDecimal safeBigDecimal(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private Date maxDate(Date first, Date second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.after(second) ? first : second;
	}

	private Date minDate(Date first, Date second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}
		return first.before(second) ? first : second;
	}

	private List<String> normalizeFilterValues(List<String> values) {
		if (values == null || values.isEmpty()) {
			return new ArrayList<String>();
		}

		LinkedHashSet<String> normalizedValues = new LinkedHashSet<String>();
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				normalizedValues.add(value.trim());
			}
		}
		return new ArrayList<String>(normalizedValues);
	}

	private static class ResolvedHistoryCriteria {
		private Date fromDate;
		private Date toDate;
		private String patientUuid;
		private BillStatus status;
		private List<String> paymentModes = new ArrayList<String>();
		private List<String> cashierUuids = new ArrayList<String>();
		private boolean emptyResult;
	}

	private static class PayeeSummary {
		private final String name;
		private final BigDecimal amount;

		private PayeeSummary(String name, BigDecimal amount) {
			this.name = name;
			this.amount = amount;
		}
	}

	/**
	 * Generate a pdf receipt
	 * @param bill The bill search settings.
	 * @return
	 */
	@Override
	public File downloadBillReceipt(Bill bill) {

		Patient patient = bill.getPatient();
		String fullName = patient.getGivenName().concat(" ").concat(
				patient.getMiddleName() != null ? bill.getPatient().getMiddleName() : ""
		).concat(" ").concat(
				patient.getFamilyName() != null ? bill.getPatient().getFamilyName() : ""
		);

        File returnFile = null;
        try {
            returnFile = File.createTempFile("patientReceipt", ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(returnFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

		PatientIdentifierType openmrsIdType = Context.getPatientService().getPatientIdentifierTypeByUuid(OPENMRS_ID);
		PatientIdentifier openmrsId = patient.getPatientIdentifier(openmrsIdType); // TODO: we should check for any NULL
        /**
		 * https://kb.itextpdf.com/home/it7kb/faq/how-to-set-the-page-size-to-envelope-size-with-landscape-orientation
		 * page size: 3.5inch length, 1.1 inch height
		 * 1mm = 0.0394 inch
		 * length = 450mm = 17.7165 inch = 127.5588 points
		 * height = 300mm = 11.811 inch = 85.0392 points
		 *
		 * The measurement system in PDF doesn't use inches, but user units. By default, 1 user unit = 1 point, and 1 inch = 72 points.
		 *
		 * Thermal printer: 4 x 10 inches paper
		 * 4 inches = 4 x 72 = 288
		 * 5 inches = 10 x 72 = 720
		 */

		int FONT_SIZE_10 = 10;
		int FONT_SIZE_8 = 8;
		int FONT_SIZE_12 = 12;
		PdfDocument pdfDoc = new PdfDocument(new PdfWriter(fos));
		Rectangle thermalPrinterPageSize = new Rectangle(288, 720);
		Document doc = new Document(pdfDoc, new PageSize(thermalPrinterPageSize));
		doc.setMargins(6,12,2,12);
		PdfFont timesRoman;
		PdfFont courier;
		PdfFont courierBold;
		PdfFont helvetica;
		PdfFont helveticaBold;
		try {
			timesRoman = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
			courierBold = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
			courier = PdfFontFactory.createFont(StandardFonts.COURIER);
			helvetica = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			helveticaBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		PdfFont headerSectionFont = helveticaBold;
		PdfFont billItemSectionFont = helvetica;
		PdfFont footerSectionFont = courierBold;
		
		// Get facility information from global property
		FacilityInfo facilityInfo = getFacilityInformation();
		Image logoImage = getLogoFromFacilityInformation();
		
		Paragraph divider = new Paragraph("------------------------------------------------------------------");
		Text billDateLabel = new Text(Utils.getSimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(bill.getDateCreated()));

		// Use facility name from facility information, fallback to location name
		GlobalProperty gp = Context.getAdministrationService().getGlobalPropertyObject(GP_DEFAULT_LOCATION);
		String facilityNameText = StringUtils.isNotEmpty(facilityInfo.facilityName) ? 
			facilityInfo.facilityName : 
			(gp != null && gp.getValue() != null ? ((Location) gp.getValue()).getName() : bill.getCashPoint().getLocation().getName());
		Text facilityName = new Text(facilityNameText);

		// Use address from facility information contacts, fallback to old global property
		String addressText = "";
		if (facilityInfo.contacts != null && StringUtils.isNotEmpty(facilityInfo.contacts.address)) {
			addressText = facilityInfo.contacts.address;
		} else {
			GlobalProperty gpFacilityAddress = Context.getAdministrationService().getGlobalPropertyObject(GP_FACILITY_ADDRESS_DETAILS);
			addressText = gpFacilityAddress != null && gpFacilityAddress.getValue() != null ? gpFacilityAddress.getPropertyValue() : "";
		}
		Text facilityAddressDetails = new Text(addressText);
		
		Paragraph logoSection = new Paragraph();
		logoSection.setFontSize(14);
		if (logoImage != null) {
			logoImage.scaleToFit(80, 80);
			logoSection.add(logoImage).add("\n");
		}
		logoSection.add(facilityName).add("\n");
		logoSection.setTextAlignment(TextAlignment.CENTER);
		logoSection.setFont(timesRoman).setBold();

		Paragraph addressSection = new Paragraph();
		addressSection.add(facilityAddressDetails).setTextAlignment(TextAlignment.CENTER).setFont(helvetica).setFontSize(12);


		float [] headerColWidth = {2f, 7f};
		Table receiptHeader = new Table(headerColWidth);
		receiptHeader.setWidth(UnitValue.createPercentValue(100f));

		receiptHeader.addCell(new Paragraph("Date:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(billDateLabel.getText())).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Receipt No:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(bill.getReceiptNumber())).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Client:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(WordUtils.capitalizeFully(fullName + " (" + patient.getAge() + " Years)"))).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);

		receiptHeader.addCell(new Paragraph("Client ID:")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(headerSectionFont);
		receiptHeader.addCell(new Paragraph(openmrsId != null ? openmrsId.getIdentifier().toUpperCase() : "")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT).setFont(helvetica);


		float[] columnWidths = { 1f, 4f, 2f, 2f, 2f, 2f };
		Table billLineItemstable = new Table(columnWidths);
		billLineItemstable.setBorder(Border.NO_BORDER);
		billLineItemstable.setWidth(UnitValue.createPercentValue(100f));

		billLineItemstable.addCell(new Paragraph("Qty").setTextAlignment(TextAlignment.LEFT)).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT);
		billLineItemstable.addCell(new Paragraph("Item").setTextAlignment(TextAlignment.LEFT)).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.LEFT);
		billLineItemstable.addCell(new Paragraph("Price")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);
		billLineItemstable.addCell(new Paragraph("Disc")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);
		billLineItemstable.addCell(new Paragraph("Tax")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);
		billLineItemstable.addCell(new Paragraph("Total")).setFontSize(FONT_SIZE_12).setTextAlignment(TextAlignment.RIGHT);

		// Only include non-voided line items in receipt
		for (BillLineItem item : bill.getLineItems()) {
			if (item != null && !item.getVoided()) {
				addBillLineItem(item, billLineItemstable, billItemSectionFont);
			}
		}

		float [] totalColWidth = {1f, 4f, 2f, 2f, 2f, 2f};
		Table totalsSection = new Table(totalColWidth);
		totalsSection.setWidth(UnitValue.createPercentValue(100f));

		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph("Subtotal")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(df.format(bill.getSubTotal()))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();

		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph("Discount")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(df.format(bill.getTotalDiscount()))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();

		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph("Tax")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(df.format(bill.getTotalTax()))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();

		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph("Total")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(" "));
		totalsSection.addCell(new Paragraph(df.format(bill.getTotal()))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();



		setInnerCellBorder(receiptHeader, Border.NO_BORDER);
		setInnerCellBorder(billLineItemstable, Border.NO_BORDER);

		float [] paymentColWidth = {1f, 5f, 2f, 2f};
		Table paymentSection = new Table(paymentColWidth);
		paymentSection.setWidth(UnitValue.createPercentValue(100f));
		paymentSection.addCell(new Paragraph("  "));
		paymentSection.addCell(new Paragraph("Payment").setTextAlignment(TextAlignment.LEFT).setBold());
		paymentSection.addCell(new Paragraph("Ref No").setTextAlignment(TextAlignment.RIGHT).setBold());
		paymentSection.addCell(new Paragraph(" "));
		// append payment rows (exclude voided payments)
		for (Payment payment : bill.getPayments()) {
			if (payment != null && !payment.getVoided()) {
				PaymentAttribute paymentReferenceAttribute = payment.getActiveAttributes().stream().filter(attribute -> attribute.getAttributeType().getUuid().equals(PAYMENT_REFERENCE_ATTRIBUTE)).findFirst().orElse(null);
				String paymentReferenceCode = "";
				if (paymentReferenceAttribute != null) {
					paymentReferenceCode = paymentReferenceAttribute.getValue();
				}
				paymentSection.addCell(new Paragraph(" "));
				paymentSection.addCell(new Paragraph(payment.getInstanceType().getName()).setTextAlignment(TextAlignment.LEFT)).setFontSize(10).setFont(helvetica);
				paymentSection.addCell(new Paragraph(paymentReferenceCode).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
				paymentSection.addCell(new Paragraph(df.format(payment.getAmountTendered())).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
			}
		}

		setInnerCellBorder(paymentSection, Border.NO_BORDER);
		setInnerCellBorder(totalsSection, Border.NO_BORDER);
		
		// Add deposits section if there are deposits
		float [] depositColWidth = {1f, 5f, 2f, 2f};
		Table depositSection = new Table(depositColWidth);
		BigDecimal totalDeposits = bill.getTotalDeposits();
		if (totalDeposits.compareTo(BigDecimal.ZERO) > 0) {
			depositSection.setWidth(UnitValue.createPercentValue(100f));
			depositSection.addCell(new Paragraph("  "));
			depositSection.addCell(new Paragraph("Deposits").setTextAlignment(TextAlignment.LEFT).setBold());
			depositSection.addCell(new Paragraph(" "));
			depositSection.addCell(new Paragraph(" "));
			
			// Get deposit service to fetch deposit details
			IDepositService depositService = Context.getService(IDepositService.class);
			List<Deposit> patientDeposits = depositService.getDepositsByPatient(bill.getPatient(), null);
			
			for (Deposit deposit : patientDeposits) {
				if (deposit.getTransactions() != null) {
					for (DepositTransaction transaction : deposit.getTransactions()) {
						if (!transaction.getVoided() &&
								transaction.getTransactionType() == TransactionType.APPLY &&
								transaction.getBillLineItem() != null &&
								bill.getLineItems().contains(transaction.getBillLineItem())) {
							depositSection.addCell(new Paragraph(" "));
							depositSection.addCell(new Paragraph("Deposit: " + deposit.getReferenceNumber()).setTextAlignment(TextAlignment.LEFT)).setFontSize(10).setFont(helvetica);
							depositSection.addCell(new Paragraph(" "));
							depositSection.addCell(new Paragraph(df.format(transaction.getAmount())).setTextAlignment(TextAlignment.RIGHT)).setFontSize(10).setFont(helvetica);
						}
					}
				}
			}
			
			setInnerCellBorder(depositSection, Border.NO_BORDER);
		}
		
		// Add payment summary section to distinguish between actual payments and waivers
		float [] summaryColWidth = {1f, 5f, 2f, 2f};
		Table paymentSummarySection = new Table(summaryColWidth);
		paymentSummarySection.setWidth(UnitValue.createPercentValue(100f));
		
		BigDecimal totalActualPayments = bill.getTotalActualPayments();
		BigDecimal totalWaivers = bill.getTotalWaivers();
		
		// Add Total Paid line (excluding waivers)
		paymentSummarySection.addCell(new Paragraph(" "));
		paymentSummarySection.addCell(new Paragraph(" "));
		paymentSummarySection.addCell(new Paragraph("Total Paid")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		paymentSummarySection.addCell(new Paragraph(df.format(totalActualPayments))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		
		// Add Total Waived line (only if there are waivers)
		if (totalWaivers.compareTo(BigDecimal.ZERO) > 0) {
			paymentSummarySection.addCell(new Paragraph(" "));
			paymentSummarySection.addCell(new Paragraph(" "));
			paymentSummarySection.addCell(new Paragraph("Total Waived")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
			paymentSummarySection.addCell(new Paragraph(df.format(totalWaivers))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
		}
		
		setInnerCellBorder(paymentSummarySection, Border.NO_BORDER);
		
		// Add balance section
		float [] balanceColWidth = {1f, 5f, 2f, 2f};
		Table balanceSection = new Table(balanceColWidth);
		BigDecimal balance = bill.getBalance();
		if (balance.compareTo(BigDecimal.ZERO) > 0) {
			balanceSection.setWidth(UnitValue.createPercentValue(100f));
			balanceSection.addCell(new Paragraph(" "));
			balanceSection.addCell(new Paragraph(" "));
			balanceSection.addCell(new Paragraph("Balance Due")).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
			balanceSection.addCell(new Paragraph(df.format(balance))).setFontSize(10).setTextAlignment(TextAlignment.RIGHT).setFont(helvetica).setBold();
			setInnerCellBorder(balanceSection, Border.NO_BORDER);
		}
		
		doc.add(logoSection);
		doc.add(addressSection);
		doc.add(receiptHeader);
		doc.add(divider);
		doc.add(billLineItemstable);
		doc.add(divider);
		doc.add(totalsSection);
		doc.add(divider);
		doc.add(paymentSection);
		doc.add(divider);
		doc.add(depositSection);
		doc.add(divider);
		doc.add(paymentSummarySection);
		doc.add(divider);
		doc.add(balanceSection);
		doc.add(divider);
		doc.add(new Paragraph("You were served by " + bill.getCashier().getName()).setFont(footerSectionFont).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
		doc.add(new Paragraph("GET WELL SOON").setFont(footerSectionFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER));

		doc.close();
		return returnFile;
	}

	private void addBillLineItem(BillLineItem item, Table table, PdfFont font) {
		if (item.getPaymentStatus().equals(BillStatus.PENDING)) { // all other statuses mean that the line item's bill is settled
			return;
		}
		String itemName = "";
		if (item.getItem() != null) {
			itemName = item.getItem().getCommonName();
		} else if (item.getBillableService() != null) {
			itemName = item.getBillableService().getName();
		}
		addFormattedCell(table, item.getQuantity().toString(), font, TextAlignment.LEFT);
		addFormattedCell(table, itemName, font, TextAlignment.LEFT);
		addFormattedCell(table, df.format(item.getPrice()), font, TextAlignment.RIGHT);
		addFormattedCell(table, df.format(item.getTotalDiscount()), font, TextAlignment.RIGHT);
		addFormattedCell(table, df.format(item.getTotalTax()), font, TextAlignment.RIGHT);
		addFormattedCell(table, df.format(item.getNetTotal()), font, TextAlignment.RIGHT);
	}

	private void addFormattedCell(Table table, String cellValue, PdfFont font, TextAlignment alignment) {
		table.addCell(new Paragraph(cellValue).setTextAlignment(alignment)).setFontSize(12).
				setTextAlignment(alignment).
				setBorder(Border.NO_BORDER).
				setFont(font);

	}

	private static void setInnerCellBorder(Table table, Border border) {
		for (IElement child : table.getChildren()) {
			if (child instanceof Cell) {
				((Cell) child).setBorder(border);
			}
		}
	}

	@Override
	@Authorized({ PrivilegeConstants.CLOSE_BILLS })
	@Transactional
	public Bill closeBill(Bill bill, String reason) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}
		
		bill.closeBill(reason);
		return super.save(bill);
	}

	@Override
	@Authorized({ PrivilegeConstants.REOPEN_BILLS })
	@Transactional
	public Bill reopenBill(Bill bill) {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}
		
		bill.reopenBill();
		return super.save(bill);
	}

	@Override
	@Authorized({ PrivilegeConstants.MANAGE_BILLS })
	@Transactional
	public Bill syncBillStatus(String billUuid) {
		if (StringUtils.isBlank(billUuid)) {
			throw new IllegalArgumentException("The bill UUID must be defined.");
		}

		Bill bill = getByUuid(billUuid);
		if (bill == null) {
			throw new IllegalArgumentException("Bill not found with UUID: " + billUuid);
		}

		if (bill.getLineItems() != null) {
			for (BillLineItem lineItem : bill.getLineItems()) {
				if (lineItem != null) {
					lineItem.normalizePriceOverride();
					lineItem.synchronizePaymentStatus();
				}
			}
		}

		bill.synchronizeBillStatus();

		if (bill.getLineItems() != null && !bill.getLineItems().isEmpty()) {
			return super.saveAll(bill, bill.getLineItems());
		}

		return super.save(bill);
	}

	/**
	 * Get logo from facility information global property
	 * @return Image object or null if not found
	 */
	private Image getLogoFromFacilityInformation() {
		Image configuredLogo = BrandingLogoProvider.loadConfiguredLogo();
		if (configuredLogo != null) {
			return configuredLogo;
		}

		try {
			String facilityInfoJson = Context.getAdministrationService()
					.getGlobalProperty(GP_FACILITY_INFORMATION);

			if (StringUtils.isNotEmpty(facilityInfoJson)) {
				JsonNode facilityNode = objectMapper.readTree(facilityInfoJson);
				
				// First try to use logo data from global property (base64 encoded)
				String logoData = getJsonValue(facilityNode, "logoData", "");
				if (StringUtils.isNotEmpty(logoData)) {
					Image logo = BrandingLogoProvider.loadBase64(logoData);
					if (logo != null) {
						return logo;
					}
				}
				
				// If no logo data, try to use logo path from global property
				String logoPath = getJsonValue(facilityNode, "logoPath", "");
				if (StringUtils.isNotEmpty(logoPath)) {
					Image logo = BrandingLogoProvider.loadLocation(logoPath);
					if (logo != null) {
						return logo;
					}
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to parse facility information JSON for logo", e);
		}

		// Fallback to the original hardcoded logo if facility information is not available
		try {
			URL logoUrl = BillServiceImpl.class.getClassLoader().getResource("img/luqman-logo-black.svg");
			if (logoUrl != null) {
				return new Image(ImageDataFactory.create(logoUrl));
			}
		} catch (Exception e) {
			LOG.warn("Failed to load fallback logo", e);
		}

		return null;
	}

	/**
	 * Safely extract value from JSON node with fallback
	 */
	private String getJsonValue(JsonNode node, String fieldName, String defaultValue) {
		return node.has(fieldName) ? node.get(fieldName).asText() : defaultValue;
	}

	/**
	 * Get facility information from global property
	 * @return FacilityInfo object with parsed facility information
	 */
	private FacilityInfo getFacilityInformation() {
		FacilityInfo info = new FacilityInfo();
		
		try {
			String facilityInfoJson = Context.getAdministrationService()
					.getGlobalProperty(GP_FACILITY_INFORMATION);

			if (StringUtils.isNotEmpty(facilityInfoJson)) {
				JsonNode facilityNode = objectMapper.readTree(facilityInfoJson);
				info.facilityName = getJsonValue(facilityNode, "facilityName", info.facilityName);
				info.tagline = getJsonValue(facilityNode, "tagline", info.tagline);
				info.logoPath = getJsonValue(facilityNode, "logoPath", info.logoPath);
				info.logoData = getJsonValue(facilityNode, "logoData", info.logoData);
				
				// Parse contacts if present
				if (facilityNode.has("contacts")) {
					JsonNode contactsNode = facilityNode.get("contacts");
					info.contacts = new FacilityContacts();
					info.contacts.tel = getJsonValue(contactsNode, "tel", "");
					info.contacts.email = getJsonValue(contactsNode, "email", "");
					info.contacts.address = getJsonValue(contactsNode, "address", "");
					info.contacts.web = getJsonValue(contactsNode, "website", "");
					info.contacts.emergency = getJsonValue(contactsNode, "emergency", "");
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to parse facility information JSON. Using defaults.", e);
		}

		return info;
	}

	/**
	 * Facility information data class
	 */
	private static class FacilityInfo {
		public String facilityName = "";
		public String tagline = "";
		public String logoPath = "";
		public String logoData = "";
		public FacilityContacts contacts = null;

		public FacilityInfo() {
		}
	}

	/**
	 * Facility contacts data class
	 */
	private static class FacilityContacts {
		public String tel = "";
		public String email = "";
		public String address = "";
		public String web = "";
		public String emergency = "";

		public boolean hasAny() {
			return StringUtils.isNotEmpty(tel) || StringUtils.isNotEmpty(email) || StringUtils.isNotEmpty(address)
					|| StringUtils.isNotEmpty(web) || StringUtils.isNotEmpty(emergency);
		}
	}
}
