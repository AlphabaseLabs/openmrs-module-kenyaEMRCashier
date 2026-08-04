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

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IPaymentModeService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.SubResource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingSubResource;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * REST resource representing a {@link Payment}.
 */
@SubResource(parent = BillResource.class, path = "payment", supportedClass = Payment.class, supportedOpenmrsVersions = {
		"2.0 - 2.*" })
public class PaymentResource extends DelegatingSubResource<Payment, Bill, BillResource> {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");

		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("instanceType", Representation.REF);
			description.addProperty("attributes");
			description.addProperty("amount");
			description.addProperty("amountTendered");
			description.addProperty("item");
			description.addProperty("allocations", Representation.DEFAULT);
			description.addProperty("dateCreated");
			description.addProperty("voided");
		}

		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("instanceType");
		description.addProperty("attributes");
		description.addProperty("amount");
		description.addProperty("amountTendered");
		description.addProperty("item");
		description.addProperty("allocations");
		description.addProperty(RestResourceConversionUtil.DATE_CREATED_PROPERTY);
		return description;
	}

	// Work around TypeVariable issue on base generic property
	// (BaseCustomizableInstanceData.getInstanceType)
	@PropertySetter("instanceType")
	public void setPaymentMode(Payment instance, String uuid) {
		IPaymentModeService service = Context.getService(IPaymentModeService.class);

		PaymentMode mode = service.getByUuid(uuid);
		if (mode == null) {
			throw new ObjectNotFoundException();
		}

		instance.setInstanceType(mode);
	}

	@PropertySetter("item")
	public void setStockItem(Payment instance, String uuid) {
		StockItem stockItem = Context.getService(StockManagementService.class).getStockItemByUuid(uuid);
		instance.setItem(stockItem);
	}

	@PropertySetter("attributes")
	public void setPaymentAttributes(Payment instance, Set<PaymentAttribute> attributes) {
		if (instance.getAttributes() == null) {
			instance.setAttributes(new HashSet<PaymentAttribute>());
		}

		BaseRestDataResource.syncCollection(instance.getAttributes(), attributes);
		for (PaymentAttribute attr : instance.getAttributes()) {
			attr.setOwner(instance);
		}
	}

	@PropertySetter("allocations")
	public void setPaymentAllocations(Payment instance, Set<LinePaymentAllocation> allocations) {
		if (allocations == null) {
			return;
		}
		if (instance.getAllocations() == null) {
			instance.setAllocations(new HashSet<LinePaymentAllocation>());
		}

		BaseRestDataResource.syncCollection(instance.getAllocations(), allocations);
		for (LinePaymentAllocation allocation : instance.getAllocations()) {
			if (allocation != null) {
				allocation.setPayment(instance);
				if (instance.getBill() != null) {
					allocation.setBill(instance.getBill());
				}
			}
		}
	}

	@PropertySetter("amount")
	public void setPaymentAmount(Payment instance, Object price) {
		instance.setAmount(RestResourceConversionUtil.toBigDecimal(price, "amount"));
	}

	@PropertySetter("amountTendered")
	public void setPaymentAmountTendered(Payment instance, Object price) {
		instance.setAmountTendered(RestResourceConversionUtil.toBigDecimal(price, "amountTendered"));
	}

	@PropertyGetter("dateCreated")
	public Long getPaymentDate(Payment instance) {
		return instance.getDateCreated() == null ? null : instance.getDateCreated().getTime();
	}

	@PropertySetter("dateCreated")
	public void setPaymentDate(Payment instance, Object date) {
		instance.setDateCreated(RestResourceConversionUtil.toDate(date));
	}

	@Override
	public Payment save(Payment delegate) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = delegate.getBill();
		bill.addPayment(delegate);
		normalizePaymentAllocations(delegate, bill);
		// Synchronize the bill status based on the current payments and deposits
		bill.synchronizeBillStatus();
		service.save(bill);

		return delegate;
	}

	@Override
	public Object update(String parentUniqueId, String uuid, SimpleObject propertiesToUpdate, RequestContext context)
	        throws ResponseException {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);
		boolean hasDateCreated = RestResourceConversionUtil.containsDateCreated(propertiesToUpdate);
		Object dateCreated = hasDateCreated ? RestResourceConversionUtil.removeDateCreated(propertiesToUpdate) : null;
		setConvertedProperties(payment, propertiesToUpdate, getUpdatableProperties(), false);
		if (hasDateCreated) {
			setPaymentDate(payment, dateCreated);
		}
		normalizePaymentAllocations(payment, bill);
		bill.synchronizeBillStatus();
		service.save(bill);
		return ConversionUtil.convertToRepresentation(payment, Representation.DEFAULT);
	}

	private void normalizePaymentAllocations(Payment payment, Bill bill) {
		if (payment == null || payment.getAllocations() == null) {
			return;
		}

		for (LinePaymentAllocation allocation : payment.getAllocations()) {
			if (allocation == null) {
				continue;
			}
			if (allocation.getAllocatedAmount() == null) {
				throw new IllegalArgumentException("Payment allocation amount must be defined.");
			}

			BillLineItem lineItem = findBillLineItem(bill, allocation);
			allocation.setBill(bill);
			allocation.setPayment(payment);
			lineItem.addAllocation(allocation);
			lineItem.synchronizePaymentStatus();
		}
	}

	private BillLineItem findBillLineItem(Bill bill, LinePaymentAllocation allocation) {
		if (bill == null || bill.getLineItems() == null || allocation == null || allocation.getBillLineItem() == null) {
			throw new IllegalArgumentException("Payment allocation billLineItem must be defined.");
		}

		String lineItemUuid = allocation.getBillLineItem().getUuid();
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem == null) {
				continue;
			}
			if (lineItem == allocation.getBillLineItem()) {
				return lineItem;
			}
			if (lineItemUuid != null && lineItemUuid.equals(lineItem.getUuid())) {
				allocation.setBillLineItem(lineItem);
				return lineItem;
			}
		}

		throw new IllegalArgumentException("Payment allocation billLineItem must belong to the bill.");
	}

	@Override
	protected void delete(Payment delegate, String reason, RequestContext context) {
		delete(delegate.getBill().getUuid(), delegate.getUuid(), reason, context);
	}

	@Override
	public void delete(String parentUniqueId, final String uuid, String reason, RequestContext context) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);

		payment.setVoided(true);
		payment.setVoidReason(reason);
		payment.setVoidedBy(Context.getAuthenticatedUser());

		// Void associated allocations
		if (payment.getAllocations() != null) {
			for (LinePaymentAllocation allocation : payment.getAllocations()) {
				if (allocation != null && !Boolean.TRUE.equals(allocation.getVoided())) {
					allocation.setVoided(true);
					allocation.setVoidReason(reason);
					allocation.setVoidedBy(Context.getAuthenticatedUser());
				}
			}
		}

		service.save(bill);
	}

	@Override
	public void purge(Payment delegate, RequestContext context) {
		purge(delegate.getBill().getUuid(), delegate.getUuid(), context);
	}

	@Override
	public void purge(String parentUniqueId, String uuid, RequestContext context) {
		IBillService service = Context.getService(IBillService.class);
		Bill bill = findBill(service, parentUniqueId);
		Payment payment = findPayment(bill, uuid);

		bill.removePayment(payment);
		service.save(bill);
	}

	@Override
	public PageableResult doGetAll(Bill parent, RequestContext context) {
		return new AlreadyPaged<Payment>(context, new ArrayList<Payment>(parent.getPayments()), false);
	}

	@Override
	public Payment getByUniqueId(String uniqueId) {
		return null;
	}

	@Override
	public Bill getParent(Payment instance) {
		return instance.getBill();
	}

	@Override
	public void setParent(Payment instance, Bill parent) {
		instance.setBill(parent);
	}

	@Override
	public Payment newDelegate() {
		return new Payment();
	}

	private Bill findBill(IBillService service, String billUUID) {
		Bill bill = service.getByUuid(billUUID);
		if (bill == null) {
			throw new ObjectNotFoundException();
		}

		return bill;
	}

	private Payment findPayment(Bill bill, final String paymentUUID) {

		for (Payment payment : bill.getPayments()) {
			if (payment != null && payment.getUuid().equals(paymentUUID)) {
				return payment;
			}
		}
		throw new ObjectNotFoundException();
	}
}
