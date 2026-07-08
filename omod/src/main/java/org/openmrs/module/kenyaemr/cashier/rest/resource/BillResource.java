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
package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.ModuleSettings;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.ICashPointService;
import org.openmrs.module.kenyaemr.cashier.api.ITimesheetService;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.IDepositService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceTax;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;
import org.openmrs.module.kenyaemr.cashier.api.model.CashPoint;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.Timesheet;
import org.openmrs.module.kenyaemr.cashier.api.model.Deposit;
import org.openmrs.module.kenyaemr.cashier.api.model.DepositTransaction;
import org.openmrs.module.kenyaemr.cashier.api.model.TransactionType;
import org.openmrs.module.kenyaemr.cashier.api.search.BillSearch;
import org.openmrs.module.kenyaemr.cashier.api.search.BillableServiceSearch;
import org.openmrs.module.kenyaemr.cashier.api.util.PaymentReplayUtil;
import org.openmrs.module.kenyaemr.cashier.api.util.RoundingUtil;
import org.openmrs.module.kenyaemr.cashier.api.base.PagingInfo;
import org.openmrs.module.kenyaemr.cashier.base.resource.AlreadyPagedWithLength;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.base.resource.PagingUtil;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;

import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST resource representing a {@link Bill}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
		+ "/bill", supportedClass = Bill.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillResource extends BaseRestDataResource<Bill> {
	private static final Log LOG = LogFactory.getLog(BillResource.class);

	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);

		if (rep instanceof RefRepresentation) {
			// For REF representation, only include basic identifying properties
			description.addProperty("uuid");
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("dateCreated");
		} else if (rep instanceof DefaultRepresentation) {
			// For DEFAULT representation, include essential properties with appropriate
			// detail levels
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint", Representation.REF);
			description.addProperty("cashier", Representation.REF);
			description.addProperty("dateCreated");
			description.addProperty("lineItems", Representation.DEFAULT);
			description.addProperty("patient", Representation.DEFAULT);
			description.addProperty("payments", Representation.DEFAULT);
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("adjustmentReason");
			description.addProperty("id");
			description.addProperty("closed");
			description.addProperty("closeReason");
			description.addProperty("closedBy");
			description.addProperty("dateClosed");
			// Add calculated properties for cumulative totals
			description.addProperty("totalPayments", findMethod("getTotalPayments"), Representation.DEFAULT);
			description.addProperty("totalActualPayments", findMethod("getTotalActualPayments"),
					Representation.DEFAULT);
			description.addProperty("totalWaivers", findMethod("getTotalWaivers"), Representation.DEFAULT);
			description.addProperty("totalExempted", findMethod("getTotalExempted"), Representation.DEFAULT);
			description.addProperty("totalDeposits", findMethod("getTotalDeposits"), Representation.DEFAULT);
			description.addProperty("total", findMethod("getTotal"), Representation.DEFAULT);
			description.addProperty("subTotal", findMethod("getSubTotal"), Representation.DEFAULT);
			description.addProperty("totalDiscount", findMethod("getTotalDiscount"), Representation.DEFAULT);
			description.addProperty("totalTax", findMethod("getTotalTax"), Representation.DEFAULT);
			description.addProperty("balance", findMethod("getBalance"), Representation.DEFAULT);
		} else if (rep instanceof FullRepresentation) {
			// Use REF for self-referential bill links to avoid recursive conversion loops
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint", Representation.FULL);
			description.addProperty("cashier", Representation.FULL);
			description.addProperty("dateCreated");
			description.addProperty("lineItems", Representation.FULL);
			description.addProperty("patient", Representation.FULL);
			description.addProperty("payments", Representation.FULL);
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("adjustmentReason");
			description.addProperty("id");
			description.addProperty("closed");
			description.addProperty("closeReason");
			description.addProperty("closedBy");
			description.addProperty("dateClosed");
			// Add calculated properties for cumulative totals
			description.addProperty("totalPayments", findMethod("getTotalPayments"), Representation.FULL);
			description.addProperty("totalActualPayments", findMethod("getTotalActualPayments"), Representation.FULL);
			description.addProperty("totalWaivers", findMethod("getTotalWaivers"), Representation.FULL);
			description.addProperty("totalExempted", findMethod("getTotalExempted"), Representation.FULL);
			description.addProperty("totalDeposits", findMethod("getTotalDeposits"), Representation.FULL);
			description.addProperty("total", findMethod("getTotal"), Representation.FULL);
			description.addProperty("subTotal", findMethod("getSubTotal"), Representation.FULL);
			description.addProperty("totalDiscount", findMethod("getTotalDiscount"), Representation.FULL);
			description.addProperty("totalTax", findMethod("getTotalTax"), Representation.FULL);
			description.addProperty("balance", findMethod("getBalance"), Representation.FULL);
		} else if (rep instanceof CustomRepresentation) {
			// For CUSTOM representation, include all properties but let the custom
			// representation handle detail levels
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint");
			description.addProperty("cashier");
			description.addProperty("dateCreated");
			description.addProperty("lineItems");
			description.addProperty("patient");
			description.addProperty("payments");
			description.addProperty("receiptNumber");
			description.addProperty("status");
			description.addProperty("adjustmentReason");
			description.addProperty("id");
			description.addProperty("closed");
			description.addProperty("closeReason");
			description.addProperty("closedBy");
			description.addProperty("dateClosed");
			// Add calculated properties for cumulative totals
			description.addProperty("totalPayments", findMethod("getTotalPayments"));
			description.addProperty("totalActualPayments", findMethod("getTotalActualPayments"));
			description.addProperty("totalWaivers", findMethod("getTotalWaivers"));
			description.addProperty("totalExempted", findMethod("getTotalExempted"));
			description.addProperty("totalDeposits", findMethod("getTotalDeposits"));
			description.addProperty("total", findMethod("getTotal"));
			description.addProperty("subTotal", findMethod("getSubTotal"));
			description.addProperty("totalDiscount", findMethod("getTotalDiscount"));
			description.addProperty("totalTax", findMethod("getTotalTax"));
			description.addProperty("balance", findMethod("getBalance"));
		}

		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		return getRepresentationDescription(new DefaultRepresentation());
	}

	@PropertySetter("lineItems")
	public void setBillLineItems(Bill instance, List<BillLineItem> lineItems) {
		if (instance.getLineItems() == null) {
			instance.setLineItems(new ArrayList<BillLineItem>());
		}

		// Clear existing line items and add new ones directly
		// This avoids issues with syncCollection when new items don't have UUIDs yet
		instance.getLineItems().clear();

		if (lineItems != null) {
			int lineItemOrder = 0;
			for (BillLineItem item : lineItems) {
				if (item != null) {
					// Validate required fields
					if (item.getPrice() == null) {
						throw new IllegalArgumentException("Line item price cannot be null");
					}
					if (item.getQuantity() == null || item.getQuantity() <= 0) {
						throw new IllegalArgumentException("Line item quantity must be greater than 0");
					}
					if (item.getPaymentStatus() == null) {
						// Set default payment status if not provided
						item.setPaymentStatus(BillStatus.PENDING);
					}

					applyTaxes(item);
					item.setBill(instance);
					// Keep line_item_order consistent with payload sequence to avoid index/property conflicts.
					item.setLineItemOrder(lineItemOrder++);
					if (item.getAdjustments() != null) {
						for (BillLineItemAdjustment adjustment : item.getAdjustments()) {
							if (adjustment != null) {
								adjustment.setBillLineItem(item);
							}
						}
					}
					instance.getLineItems().add(item);
				}
			}
		}
	}

	private void applyTaxes(BillLineItem item) {
		if (item == null) {
			LOG.info("applyTaxes: item is null, skipping");
			return;
		}
		LOG.info("applyTaxes: processing item uuid=" + item.getUuid() +
				" billableService=" + (item.getBillableService() != null ? item.getBillableService().getUuid() : "null") +
				" itemPrice=" + (item.getItemPrice() != null ? item.getItemPrice().getUuid() : "null") +
				" stockItem=" + (item.getItem() != null ? item.getItem().getUuid() : "null"));

		// Try to resolve billableService from various sources
		if (item.getBillableService() == null) {
			// First, try to resolve from itemPrice (CashierItemPrice has billableService reference)
			if (item.getItemPrice() != null) {
				BillableService serviceFromPrice = item.getItemPrice().getBillableService();
				LOG.info("applyTaxes: itemPrice.getBillableService()=" + (serviceFromPrice != null ? serviceFromPrice.getUuid() : "null"));
				if (serviceFromPrice != null) {
					// Re-fetch the BillableService to ensure serviceTaxes collection is properly loaded
					IBillableItemsService billableItemsService = Context.getService(IBillableItemsService.class);
					BillableService fullService = billableItemsService.getByUuid(serviceFromPrice.getUuid());
					if (fullService != null) {
						item.setBillableService(fullService);
						LOG.info("applyTaxes: resolved billableService from itemPrice (re-fetched)=" + fullService.getUuid());
					} else {
						item.setBillableService(serviceFromPrice);
						LOG.info("applyTaxes: resolved billableService from itemPrice (proxy)=" + serviceFromPrice.getUuid());
					}
				}
			}
			// Second, try to resolve from stock item
			if (item.getBillableService() == null && item.getItem() != null) {
				BillableService service = resolveBillableService(item);
				if (service != null) {
					item.setBillableService(service);
					LOG.info("applyTaxes: resolved billableService from stockItem=" + service.getUuid());
				} else {
					LOG.info("applyTaxes: no billableService found for stock item " + item.getItem().getUuid());
				}
			}
		} else {
			// BillableService is already set, but might be a proxy - re-fetch to ensure serviceTaxes are loaded
			IBillableItemsService billableItemsService = Context.getService(IBillableItemsService.class);
			BillableService fullService = billableItemsService.getByUuid(item.getBillableService().getUuid());
			if (fullService != null) {
				item.setBillableService(fullService);
				LOG.info("applyTaxes: re-fetched existing billableService=" + fullService.getUuid());
			}
		}
		if (item.getBillableService() == null) {
			LOG.info("applyTaxes: billableService is null, skipping tax calculation");
			return;
		}
		if (item.getPrice() == null || item.getQuantity() == null) {
			LOG.info("applyTaxes: missing price or quantity, skipping. price=" + item.getPrice()
					+ " quantity=" + item.getQuantity());
			return;
		}

		BigDecimal baseAmount = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		if (baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
			LOG.info("applyTaxes: baseAmount <= 0, skipping. baseAmount=" + baseAmount);
			return;
		}

		List<BillLineItemAdjustment> adjustments = item.getAdjustments();
		if (adjustments == null) {
			adjustments = new ArrayList<BillLineItemAdjustment>();
			item.setAdjustments(adjustments);
			LOG.info("applyTaxes: created adjustments list");
		} else {
			Iterator<BillLineItemAdjustment> iterator = adjustments.iterator();
			int removed = 0;
			while (iterator.hasNext()) {
				BillLineItemAdjustment existing = iterator.next();
				if (existing != null && "TAX".equalsIgnoreCase(existing.getAdjustmentType())) {
					iterator.remove();
					removed++;
				}
			}
			if (removed > 0) {
				LOG.info("applyTaxes: removed existing TAX adjustments=" + removed);
			}
		}

		BigDecimal discountAmount = calculateDiscountAmount(adjustments);
		BigDecimal taxableAmount = baseAmount.subtract(discountAmount);
		if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
			taxableAmount = BigDecimal.ZERO;
		}
		LOG.info("applyTaxes: baseAmount=" + baseAmount + " discountAmount=" + discountAmount
				+ " taxableAmount=" + taxableAmount);
		if (taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {
			LOG.info("applyTaxes: taxableAmount <= 0, skipping tax generation");
			return;
		}

		List<BillableServiceTax> serviceTaxes = item.getBillableService().getServiceTaxes();
		if (serviceTaxes == null) {
			LOG.info("applyTaxes: no serviceTaxes for billableService=" + item.getBillableService().getUuid());
			return;
		}
		LOG.info("applyTaxes: serviceTaxes count=" + serviceTaxes.size());
		for (BillableServiceTax serviceTax : serviceTaxes) {
			if (serviceTax == null || serviceTax.getVoided()) {
				LOG.info("applyTaxes: skipping voided or null serviceTax");
				continue;
			}
			CashierTaxType taxType = serviceTax.getTaxType();
			BigDecimal rate = serviceTax.getOverrideRate() != null ? serviceTax.getOverrideRate()
					: (taxType == null ? null : taxType.getRate());
			if (rate == null) {
				LOG.info("applyTaxes: missing rate for serviceTax id=" + serviceTax.getId());
				continue;
			}

			BigDecimal amount = taxableAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
			BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
			adjustment.setAdjustmentType("TAX");
			adjustment.setTaxType(taxType);
			adjustment.setRate(rate);
			adjustment.setAmount(amount);
			adjustment.setBaseAmount(taxableAmount);
			adjustment.setBillLineItem(item);
			// Set required BaseOpenmrsData fields for Hibernate persistence
			adjustment.setCreator(Context.getAuthenticatedUser());
			adjustment.setDateCreated(new Date());
			adjustment.setVoided(false);
			adjustment.setUuid(UUID.randomUUID().toString());
			adjustments.add(adjustment);
			LOG.info("applyTaxes: added TAX adjustment amount=" + amount + " rate=" + rate + " uuid=" + adjustment.getUuid());
		}
	}

	private BigDecimal calculateDiscountAmount(List<BillLineItemAdjustment> adjustments) {
		BigDecimal discountAmount = BigDecimal.ZERO;
		if (adjustments == null) {
			return discountAmount;
		}
		for (BillLineItemAdjustment adjustment : adjustments) {
			if (adjustment == null || adjustment.getVoided()) {
				continue;
			}
			if (!"DISCOUNT".equalsIgnoreCase(adjustment.getAdjustmentType())) {
				continue;
			}
			BigDecimal amount = adjustment.getAmount();
			if (amount == null && adjustment.getBaseAmount() != null && adjustment.getRate() != null) {
				amount = adjustment.getBaseAmount().multiply(adjustment.getRate());
			}
			if (amount != null) {
				discountAmount = discountAmount.add(amount);
			}
		}
		return discountAmount;
	}

	private BillableService resolveBillableService(BillLineItem item) {
		if (item == null || item.getItem() == null) {
			return null;
		}
		BillableService template = new BillableService();
		template.setStockItem(item.getItem());
		template.setServiceStatus(BillableServiceStatus.ENABLED);
		IBillableItemsService service = Context.getService(IBillableItemsService.class);
		List<BillableService> results = service.findServices(new BillableServiceSearch(template));
		return results == null || results.isEmpty() ? null : results.get(0);
	}

	@PropertySetter("payments")
	public void setBillPayments(Bill instance, Set<Payment> payments) {
		if (payments == null) {
			return;
		}

		if (instance.getPayments() == null) {
			instance.setPayments(new HashSet<Payment>());
		}

		Set<Integer> existingPaymentIds = new HashSet<Integer>();
		Set<String> existingPaymentUuids = new HashSet<String>();
		Set<String> persistedPaymentAttributeKeys = new HashSet<String>();
		Set<String> persistedPaymentReplaySignatures = new HashSet<String>();
		for (Payment existing : instance.getPayments()) {
			if (existing == null) {
				continue;
			}
			if (existing.getId() != null) {
				existingPaymentIds.add(existing.getId());
			}
			String existingUuid = StringUtils.trimToNull(existing.getUuid());
			if (existingUuid != null) {
				existingPaymentUuids.add(existingUuid);
			}
			if (existing.getId() != null && !Boolean.TRUE.equals(existing.getVoided())) {
				persistedPaymentAttributeKeys.addAll(PaymentReplayUtil.getAttributeValueKeys(existing));
				String replaySignature = PaymentReplayUtil.getReplaySignature(existing);
				if (replaySignature != null) {
					persistedPaymentReplaySignatures.add(replaySignature);
				}
			}
		}

		// Only add NEW payments that do not match an existing persisted payment.
		// This handles idempotent client retries where payment UUID is missing but unique attributes
		// (e.g. Transaction ID) already exist on the bill.
		for (Payment payment : payments) {
			if (payment == null) {
				continue;
			}

			if (payment.getId() != null && existingPaymentIds.contains(payment.getId())) {
				continue;
			}

			String paymentUuid = StringUtils.trimToNull(payment.getUuid());
			if (paymentUuid != null && existingPaymentUuids.contains(paymentUuid)) {
				continue;
			}

			String incomingReplaySignature = PaymentReplayUtil.getReplaySignature(payment);
			if (incomingReplaySignature != null && persistedPaymentReplaySignatures.contains(incomingReplaySignature)) {
				LOG.info("Skipping duplicate incoming payment for bill " + instance.getUuid()
				        + " (paymentId=" + payment.getId() + ", paymentUuid=" + payment.getUuid() + ")");
				continue;
			}

			Set<String> incomingAttributeKeys = PaymentReplayUtil.getAttributeValueKeys(payment);
			boolean duplicatesPersistedPayment = false;
			for (String incomingKey : incomingAttributeKeys) {
				if (persistedPaymentAttributeKeys.contains(incomingKey)) {
					duplicatesPersistedPayment = true;
					break;
				}
			}
			if (duplicatesPersistedPayment) {
				LOG.info("Skipping duplicate incoming payment for bill " + instance.getUuid()
				        + " (paymentId=" + payment.getId() + ", paymentUuid=" + payment.getUuid() + ")");
				continue;
			}

			// New payment — link and add
			payment.setBill(instance);
			if (payment.getAttributes() != null) {
				for (PaymentAttribute attr : payment.getAttributes()) {
					if (attr != null) {
						attr.setOwner(payment);
					}
				}
			}
			instance.addPayment(payment);

			if (payment.getId() != null) {
				existingPaymentIds.add(payment.getId());
			}
			if (paymentUuid != null) {
				existingPaymentUuids.add(paymentUuid);
			}
			if (incomingReplaySignature != null) {
				persistedPaymentReplaySignatures.add(incomingReplaySignature);
			}
		}

		// Recompute status once after all payments are linked.
		instance.synchronizeBillStatus();
	}

	@PropertySetter("billAdjusted")
	public void setBillAdjusted(Bill instance, Bill billAdjusted) {
		billAdjusted.addAdjustedBy(instance);
		instance.setBillAdjusted(billAdjusted);
	}

	@PropertySetter("status")
	public void setBillStatus(Bill instance, BillStatus status) {
		if (instance.getStatus() == null) {
			instance.setStatus(status);
		} else if (instance.getStatus() == BillStatus.PENDING && status == BillStatus.POSTED) {
			instance.setStatus(status);
		}
		if (status == BillStatus.POSTED) {
			RoundingUtil.handleRoundingLineItem(instance);
		}
	}

	@PropertySetter("adjustmentReason")
	public void setAdjustReason(Bill instance, String adjustReason) {
		if (instance.getBillAdjusted().getUuid() != null) {
			instance.getBillAdjusted().setAdjustmentReason(adjustReason);
		}
	}

	@PropertyGetter("lineItems")
	public List<BillLineItem> getLineItems(Bill instance) {
		if (instance.getLineItems() == null) {
			return new ArrayList<>();
		}

		// Check the includeVoided parameter from the current HTTP request
		boolean includeVoided = false;
		try {
			org.springframework.web.context.request.ServletRequestAttributes attributes = 
				(org.springframework.web.context.request.ServletRequestAttributes) 
				org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
			
			if (attributes != null) {
				javax.servlet.http.HttpServletRequest request = attributes.getRequest();
				String includeVoidedStr = request.getParameter("includeVoided");
				includeVoided = Strings.isNotEmpty(includeVoidedStr) && Boolean.parseBoolean(includeVoidedStr);
			}
		} catch (Exception e) {
			// If we can't get the request context, default to including all items
			includeVoided = true;
		}

		// Filter out voided items if includeVoided is false
		if (!includeVoided) {
			return instance.getLineItems().stream()
					.filter(item -> item == null || !item.getVoided())
					.collect(java.util.stream.Collectors.toList());
		}

		return new ArrayList<>(instance.getLineItems());
	}

	@PropertyGetter("payments")
	public Set<Payment> getPayments(Bill instance) {
		if (instance.getPayments() == null) {
			return new HashSet<>();
		}

		// Check the includeVoided parameter from the current HTTP request
		boolean includeVoided = false;
		try {
			org.springframework.web.context.request.ServletRequestAttributes attributes = 
				(org.springframework.web.context.request.ServletRequestAttributes) 
				org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
			
			if (attributes != null) {
				javax.servlet.http.HttpServletRequest request = attributes.getRequest();
				String includeVoidedStr = request.getParameter("includeVoided");
				includeVoided = Strings.isNotEmpty(includeVoidedStr) && Boolean.parseBoolean(includeVoidedStr);
			}
		} catch (Exception e) {
			// If we can't get the request context, default to including all items
			includeVoided = true;
		}

		// Filter out voided payments if includeVoided is false
		if (!includeVoided) {
			return instance.getPayments().stream()
					.filter(payment -> payment == null || !payment.getVoided())
					.collect(java.util.stream.Collectors.toSet());
		}

		return new HashSet<>(instance.getPayments());
	}

	@Override
	public Bill save(Bill bill) {
		// TODO: Test all the ways that this could fail

		if (bill.getId() == null) {
			if (bill.getCashier() == null) {
				Provider cashier = getCurrentCashier(bill);
				if (cashier == null) {
					throw new RestClientException("Couldn't find Provider for the current user ("
							+ Context.getAuthenticatedUser().getUsername() + ")");
				}

				bill.setCashier(cashier);
			}

			if (bill.getCashPoint() == null) {
				loadBillCashPoint(bill);
			}

			// Now that all all attributes have been set (i.e., payments and bill status) we
			// can check to see if the bill
			// is fully paid.
			bill.synchronizeBillStatus();
			if (bill.getStatus() == null) {
				bill.setStatus(BillStatus.PENDING);
			}
		}

		return super.save(bill);
	}

	@Override
	protected AlreadyPaged<Bill> doSearch(RequestContext context) {
		BillSearchRequest searchRequest = BillSearchRequest.from(context);
		PagingInfo pagingInfo = buildPagingInfo(context, searchRequest.requestTotalCount);
		List<Bill> result = Context.getService(IBillService.class).getBills(buildBillSearch(searchRequest), pagingInfo);
		return buildPagedResponse(context, result, pagingInfo);
	}

	private BillSearch buildBillSearch(BillSearchRequest searchRequest) {
		Bill searchTemplate = new Bill();
		searchTemplate.setPatient(resolvePatient(searchRequest.patientUuid));
		searchTemplate.setReceiptNumber(StringUtils.isNotBlank(searchRequest.receiptNumber) ? searchRequest.receiptNumber
		        .trim() : null);
		searchTemplate.setStatus(resolveBillStatus(searchRequest.status));
		searchTemplate.setCashPoint(resolveCashPoint(searchRequest.cashPointUuid));

		return new BillSearch(searchTemplate, parseDate(searchRequest.createdOnOrAfter),
		    parseDate(searchRequest.createdOnOrBefore), Boolean.valueOf(searchRequest.includeVoidedBills),
		    Boolean.valueOf(searchRequest.includeClosedBills));
	}

	private PagingInfo buildPagingInfo(RequestContext context, boolean requestTotalCount) {
		if (context.getLimit() == null || context.getLimit() <= 0) {
			return null;
		}

		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);
		if (!requestTotalCount) {
			pagingInfo.setLoadRecordCount(false);
		}
		return pagingInfo;
	}

	private AlreadyPaged<Bill> buildPagedResponse(RequestContext context, List<Bill> results, PagingInfo pagingInfo) {
		if (pagingInfo != null && pagingInfo.getTotalRecordCount() != null) {
			Boolean hasMoreResults = pagingInfo.hasMoreResults();
			return new AlreadyPagedWithLength<Bill>(context, results, hasMoreResults != null && hasMoreResults,
			    pagingInfo.getTotalRecordCount());
		}
		return new AlreadyPaged<Bill>(context, results, false);
	}

	private Patient resolvePatient(String patientUuid) {
		return Strings.isNotEmpty(patientUuid) ? Context.getPatientService().getPatientByUuid(patientUuid) : null;
	}

	private CashPoint resolveCashPoint(String cashPointUuid) {
		return Strings.isNotEmpty(cashPointUuid) ? Context.getService(ICashPointService.class).getByUuid(cashPointUuid)
		        : null;
	}

	private BillStatus resolveBillStatus(String status) {
		return Strings.isNotEmpty(status) ? BillStatus.valueOf(status.toUpperCase()) : null;
	}

	private Date parseDate(String value) {
		return StringUtils.isNotBlank(value) ? (Date) ConversionUtil.convert(value, Date.class) : null;
	}

	private static class BillSearchRequest {
		private String patientUuid;
		private String receiptNumber;
		private String status;
		private String cashPointUuid;
		private String createdOnOrBefore;
		private String createdOnOrAfter;
		private boolean includeVoidedBills;
		private boolean includeClosedBills = true;
		private boolean requestTotalCount;

		private static BillSearchRequest from(RequestContext context) {
			BillSearchRequest request = new BillSearchRequest();
			request.patientUuid = context.getRequest().getParameter("patientUuid");
			request.receiptNumber = context.getRequest().getParameter("receiptNumber");
			request.status = context.getRequest().getParameter("status");
			request.cashPointUuid = context.getRequest().getParameter("cashPointUuid");
			request.createdOnOrBefore = context.getRequest().getParameter("createdOnOrBefore");
			request.createdOnOrAfter = context.getRequest().getParameter("createdOnOrAfter");
			request.includeVoidedBills = parseBoolean(context.getRequest().getParameter("includeVoidedBills"), false);
			request.includeClosedBills = parseBoolean(context.getRequest().getParameter("includeClosedBills"), true);
			request.requestTotalCount = parseBoolean(context.getRequest().getParameter("totalCount"), false);
			return request;
		}

		private static boolean parseBoolean(String value, boolean defaultValue) {
			return Strings.isNotEmpty(value) ? Boolean.parseBoolean(value) : defaultValue;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<IEntityDataService<Bill>> getServiceClass() {
		return (Class<IEntityDataService<Bill>>) (Object) IBillService.class;
	}

	public String getDisplayString(Bill instance) {
		return instance.getReceiptNumber();
	}

	@Override
	public Bill newDelegate() {
		return new Bill();
	}

	private Provider getCurrentCashier(Bill bill) {
		User currentUser = Context.getAuthenticatedUser();
		ProviderService service = Context.getProviderService();
		Collection<Provider> providers = service.getProvidersByPerson(currentUser.getPerson());
		if (!providers.isEmpty()) {
			return providers.iterator().next();
		}
		return null;
	}

	private void loadBillCashPoint(Bill bill) {
		ITimesheetService service = Context.getService(ITimesheetService.class);
		Timesheet timesheet = service.getCurrentTimesheet(bill.getCashier());
		if (timesheet == null) {
			AdministrationService adminService = Context.getAdministrationService();
			boolean timesheetRequired;
			try {
				timesheetRequired = Boolean
						.parseBoolean(adminService.getGlobalProperty(ModuleSettings.TIMESHEET_REQUIRED_PROPERTY));
			} catch (Exception e) {
				timesheetRequired = false;
			}

			if (timesheetRequired) {
				throw new RestClientException("A current timesheet does not exist for cashier " + bill.getCashier());
			} else if (bill.getBillAdjusted() != null) {
				// If this is an adjusting bill, copy cash point from billAdjusted
				bill.setCashPoint(bill.getBillAdjusted().getCashPoint());
			} else {
				throw new RestClientException("Cash point cannot be null!");
			}
		} else {
			CashPoint cashPoint = timesheet.getCashPoint();
			if (cashPoint == null) {
				throw new RestClientException("No cash points defined for the current timesheet!");
			}
			bill.setCashPoint(cashPoint);
		}
	}

	@PropertyGetter("totalPayments")
	public BigDecimal getTotalPayments(Bill instance) {
		return instance.getTotalPayments();
	}

	@PropertyGetter("totalActualPayments")
	public BigDecimal getTotalActualPayments(Bill instance) {
		return instance.getTotalActualPayments();
	}

	@PropertyGetter("totalWaivers")
	public BigDecimal getTotalWaivers(Bill instance) {
		return instance.getTotalWaivers();
	}

	@PropertyGetter("totalExempted")
	public BigDecimal getTotalExempted(Bill instance) {
		return instance.getTotalExempted();
	}

	@PropertyGetter("totalDeposits")
	public BigDecimal getTotalDeposits(Bill instance) {
		return instance.getTotalDeposits();
	}

	@PropertyGetter("total")
	public BigDecimal getTotal(Bill instance) {
		return instance.getTotal();
	}

	@PropertyGetter("subTotal")
	public BigDecimal getSubTotal(Bill instance) {
		return instance.getSubTotal();
	}

	@PropertyGetter("totalDiscount")
	public BigDecimal getTotalDiscount(Bill instance) {
		return instance.getTotalDiscount();
	}

	@PropertyGetter("totalTax")
	public BigDecimal getTotalTax(Bill instance) {
		return instance.getTotalTax();
	}

	@PropertyGetter("balance")
	public BigDecimal getBalance(Bill instance) {
		return instance.getBalance();
	}

}
