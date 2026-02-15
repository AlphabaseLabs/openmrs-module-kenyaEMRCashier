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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.util.Strings;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.BillLineItemService;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.IBillableItemsService;
import org.openmrs.module.kenyaemr.cashier.api.ICashierTaxTypeService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableService;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierItemPrice;
import org.openmrs.module.kenyaemr.cashier.api.search.BillItemSearch;
import org.openmrs.module.kenyaemr.cashier.base.resource.BaseRestDataResource;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.api.base.entity.IEntityDataService;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.stockmanagement.api.StockManagementService;
import org.openmrs.module.stockmanagement.api.model.StockItem;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.kenyaemr.cashier.api.ICashierItemPriceService;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST resource representing a {@link BillLineItem}.
 */
@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
        + "/billLineItem", supportedClass = BillLineItem.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class BillLineItemResource extends BaseRestDataResource<BillLineItem> {

    private static final Log LOG = LogFactory.getLog(BillLineItemResource.class);

    @Override
    public String getDisplayString(BillLineItem instance) {
        if (instance.getBillableService() != null) {
            return instance.getBillableService().getName();
        }
        if (instance.getItem() != null && instance.getItem().getCommonName() != null) {
            return instance.getItem().getCommonName();
        }
        return "BillLineItem";
    }

    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = super.getRepresentationDescription(rep);
        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("item");
            description.addProperty("billableService", Representation.REF);
            description.addProperty("quantity");
            description.addProperty("price");
            description.addProperty("priceName");
            description.addProperty("priceUuid");
            description.addProperty("lineItemOrder");
            description.addProperty("paymentStatus");
            description.addProperty("itemOrServiceConceptUuid");
            description.addProperty("serviceTypeUuid");
            description.addProperty("order", Representation.REF);
            description.addProperty("discounts");
            description.addProperty("taxes");
            description.addProperty("amount");
            description.addProperty("totalDiscount");
            description.addProperty("totalTax");
            description.addProperty("total");
            description.addProperty("totalAllocated");
        }
        return description;
    }

    @PropertySetter(value = "item")
    public void setItem(BillLineItem instance, Object item) {
        StockManagementService service = Context.getService(StockManagementService.class);
        String itemUuid = (String) item;
        instance.setItem(service.getStockItemByUuid(itemUuid));
    }

    @PropertySetter(value = "billableService")
    public void setBillableService(BillLineItem instance, Object item) {
        IBillableItemsService service = Context.getService(IBillableItemsService.class);
        String serviceUuid = (String) item;
        instance.setBillableService(service.getByUuid(serviceUuid));
    }

    @PropertyGetter(value = "item")
    public String getItem(BillLineItem instance) {
        try {
            StockItem stockItem = instance.getItem();
            return stockItem.getUuid() + ":" + stockItem.getConcept().getDisplayString();
        } catch (Exception e) {
            return "";
        }
    }

    @PropertyGetter(value = "billableService")
    public String getBillableService(BillLineItem instance) {
        try {
            BillableService service = instance.getBillableService();
            return service.getUuid() + ":" + service.getName();
        } catch (Exception e) {
            return "";
        }
    }

    @PropertySetter(value = "price")
    public void setPriceValue(BillLineItem instance, Object price) {
        if (price instanceof Double || price instanceof Integer) {
            double priceValue = ((Number) price).doubleValue();
            instance.setPrice(BigDecimal.valueOf(priceValue));
        } else {
            throw new IllegalArgumentException("Unsupported price type: " + price.getClass().getName());
        }
    }

    @PropertySetter(value = "priceName")
    public void setPriceName(BillLineItem instance, String name) {
        instance.setPriceName(name);
    }

    @PropertyGetter(value = "priceName")
    public String getPriceName(BillLineItem instance) {
        String itemName = instance.getPriceName();
        return StringUtils.isNotBlank(itemName) ? itemName : "";
    }

    @PropertySetter(value = "priceUuid")
    public void setItemPrice(BillLineItem instance, String uuid) {
        if (StringUtils.isNotBlank(uuid)) {
            try {
                ICashierItemPriceService itemPriceService = Context.getService(ICashierItemPriceService.class);
                CashierItemPrice itemPrice = itemPriceService.getByUuid(uuid);
                if (itemPrice != null) {
                    instance.setItemPrice(itemPrice);
                    if (instance.getPrice() == null) {
                        instance.setPrice(itemPrice.getPrice());
                    }
                    instance.setPriceName(itemPrice.getName());
                    // Also set billableService from itemPrice if not already set
                    if (instance.getBillableService() == null && itemPrice.getBillableService() != null) {
                        instance.setBillableService(itemPrice.getBillableService());
                        LOG.info("setItemPrice: resolved billableService from itemPrice=" + itemPrice.getBillableService().getUuid());
                    }
                } else {
                    LOG.warn("CashierItemPrice not found for UUID: " + uuid);
                }
            } catch (Exception e) {
                LOG.error("Error retrieving CashierItemPrice for UUID: " + uuid, e);
            }
        }
    }

    @PropertyGetter(value = "priceUuid")
    public String getItemPriceUuid(BillLineItem instance) {
        try {
            CashierItemPrice itemPrice = instance.getItemPrice();
            if (itemPrice == null) {
                return "";
            }
            // Force initialization of the proxy to detect orphaned references
            return itemPrice.getUuid();
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Handle orphaned foreign key reference - the itemPrice record was deleted
            LOG.warn("Orphaned itemPrice reference for bill line item " + instance.getUuid() +
                    ". The referenced CashierItemPrice record no longer exists.");
            // Clear the orphaned reference
            instance.setItemPrice(null);
            return "";
        } catch (Exception e) {
            LOG.warn("Error getting itemPrice UUID for bill line item " + instance.getUuid(), e);
            return "";
        }
    }

    @PropertyGetter(value = "itemOrServiceConceptUuid")
    public String getItemOrServiceConceptUuid(BillLineItem instance) {
        try {
            return instance.getItemOrServiceConceptUuid();
        } catch (Exception e) {
            LOG.warn("Error getting itemOrServiceConceptUuid for bill line item " + instance.getUuid(), e);
            return null;
        }
    }

    @PropertyGetter("discounts")
    public List<Map<String, Object>> getDiscounts(BillLineItem instance) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (instance.getAdjustments() == null) {
            return result;
        }

        for (BillLineItemAdjustment adjustment : instance.getAdjustments()) {
            if (adjustment == null || adjustment.getVoided()) {
                continue;
            }
            if (!"DISCOUNT".equalsIgnoreCase(adjustment.getAdjustmentType())) {
                continue;
            }
            java.util.HashMap<String, Object> discount = new java.util.HashMap<>();
            discount.put("uuid", adjustment.getUuid());
            discount.put("amount", adjustment.getAmount());
            discount.put("baseAmount", adjustment.getBaseAmount());
            discount.put("rate", adjustment.getRate());
            discount.put("description", adjustment.getDescription());
            if (adjustment.getDiscountSponsor() != null) {
                discount.put("sponsor", adjustment.getDiscountSponsor().getUuid());
            }
            result.add(discount);
        }

        return result;
    }

    @PropertyGetter("taxes")
    public List<Map<String, Object>> getTaxes(BillLineItem instance) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (instance.getAdjustments() == null) {
            return result;
        }

        for (BillLineItemAdjustment adjustment : instance.getAdjustments()) {
            if (adjustment == null || adjustment.getVoided()) {
                continue;
            }
            if (!"TAX".equalsIgnoreCase(adjustment.getAdjustmentType())) {
                continue;
            }
            java.util.HashMap<String, Object> tax = new java.util.HashMap<>();
            tax.put("uuid", adjustment.getUuid());
            tax.put("amount", adjustment.getAmount());
            tax.put("baseAmount", adjustment.getBaseAmount());
            tax.put("rate", adjustment.getRate());
            if (adjustment.getTaxType() != null && adjustment.getTaxType().getConcept() != null) {
                tax.put("concept", adjustment.getTaxType().getConcept().getUuid());
                tax.put("conceptDisplay", adjustment.getTaxType().getConcept().getName(Context.getLocale()));
            }
            result.add(tax);
        }

        return result;
    }

    @PropertyGetter("amount")
    public BigDecimal getAmount(BillLineItem instance) {
        return instance.getSubTotal();
    }

    @PropertyGetter("totalDiscount")
    public BigDecimal getTotalDiscount(BillLineItem instance) {
        return instance.getTotalDiscount();
    }

    @PropertyGetter("totalTax")
    public BigDecimal getTotalTax(BillLineItem instance) {
        return instance.getTotalTax();
    }

    @PropertyGetter("total")
    public BigDecimal getTotal(BillLineItem instance) {
        return instance.getTotal();
    }

    @PropertyGetter("totalAllocated")
    public BigDecimal getTotalAllocated(BillLineItem instance) {
        return instance.getTotalAllocated();
    }

    @PropertySetter("totalAllocated")
    public void setTotalAllocated(BillLineItem instance, Object value) {
        // Derived field; computed from allocations. Ignore client payload.
    }

    @PropertySetter("amount")
    public void setAmount(BillLineItem instance, Object amount) {
        // Derived field (price * quantity); ignore client payload value.
    }

    @PropertySetter("totalDiscount")
    public void setTotalDiscount(BillLineItem instance, Object totalDiscount) {
        // Derived field; discounts are provided via the discounts collection.
    }

    @PropertySetter("totalTax")
    public void setTotalTax(BillLineItem instance, Object totalTax) {
        // Derived field; taxes are derived/recalculated server-side.
    }

    @PropertySetter("total")
    public void setTotal(BillLineItem instance, Object total) {
        // Derived field; ignore client payload value.
    }

    @PropertySetter("discounts")
    public void setDiscounts(BillLineItem instance, List<Map<String, Object>> discounts) {
        if (discounts == null) {
            return;
        }

        List<BillLineItemAdjustment> mapped = new ArrayList<>(discounts.size());
        for (Map<String, Object> discountMap : discounts) {
            if (discountMap == null) {
                continue;
            }
            BigDecimal amount = parseBigDecimal(discountMap.get("amount"));
            if (amount == null) {
                throw new IllegalArgumentException("Discount amount is required");
            }
            BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
            adjustment.setAdjustmentType("DISCOUNT");
            adjustment.setAmount(amount);
            adjustment.setBaseAmount(parseBigDecimal(discountMap.get("baseAmount")));
            adjustment.setRate(parseBigDecimal(discountMap.get("rate")));
            adjustment.setDescription(getStringValue(discountMap.get("description")));
            adjustment.setDiscountSponsor(resolveProvider(discountMap.get("sponsor")));
            adjustment.setBillLineItem(instance);
            // Set required BaseOpenmrsData fields for Hibernate persistence
            adjustment.setCreator(Context.getAuthenticatedUser());
            adjustment.setDateCreated(new Date());
            adjustment.setVoided(false);
            adjustment.setUuid(UUID.randomUUID().toString());
            mapped.add(adjustment);
        }

        if (instance.getAdjustments() == null) {
            instance.setAdjustments(new ArrayList<>(mapped.size()));
        }

        List<BillLineItemAdjustment> existingDiscounts = new ArrayList<>();
        List<BillLineItemAdjustment> nonDiscounts = new ArrayList<>();
        for (BillLineItemAdjustment existing : instance.getAdjustments()) {
            if (existing != null && "DISCOUNT".equalsIgnoreCase(existing.getAdjustmentType())) {
                existingDiscounts.add(existing);
            } else if (existing != null) {
                nonDiscounts.add(existing);
            }
        }

        BaseRestDataResource.syncCollection(existingDiscounts, mapped);
        instance.getAdjustments().clear();
        instance.getAdjustments().addAll(nonDiscounts);
        instance.getAdjustments().addAll(existingDiscounts);

        for (BillLineItemAdjustment adjustment : instance.getAdjustments()) {
            adjustment.setBillLineItem(instance);
        }
    }

    @PropertySetter("taxes")
    public void setTaxes(BillLineItem instance, List<Map<String, Object>> taxes) {
        if (taxes == null) {
            return;
        }

        List<BillLineItemAdjustment> mapped = new ArrayList<>(taxes.size());
        ICashierTaxTypeService taxTypeService = Context.getService(ICashierTaxTypeService.class);

        for (Map<String, Object> taxMap : taxes) {
            if (taxMap == null) {
                continue;
            }
            BigDecimal amount = parseBigDecimal(taxMap.get("amount"));
            if (amount == null) {
                throw new IllegalArgumentException("Tax amount is required");
            }

            String conceptUuid = extractUuid(taxMap.get("concept"));
            if (StringUtils.isBlank(conceptUuid)) {
                conceptUuid = extractUuid(taxMap.get("taxType"));
            }
            if (StringUtils.isBlank(conceptUuid)) {
                conceptUuid = getStringValue(taxMap.get("taxTypeUuid"));
            }

            CashierTaxType taxType = conceptUuid == null ? null : taxTypeService.getByConceptUuid(conceptUuid);
            if (taxType == null) {
                throw new IllegalArgumentException("Missing or invalid tax concept for taxes");
            }

            BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
            adjustment.setAdjustmentType("TAX");
            adjustment.setAmount(amount);
            adjustment.setBaseAmount(parseBigDecimal(taxMap.get("baseAmount")));
            adjustment.setRate(parseBigDecimal(taxMap.get("rate")));
            adjustment.setDescription(getStringValue(taxMap.get("description")));
            adjustment.setTaxType(taxType);
            adjustment.setBillLineItem(instance);
            // Set required BaseOpenmrsData fields for Hibernate persistence
            adjustment.setCreator(Context.getAuthenticatedUser());
            adjustment.setDateCreated(new Date());
            adjustment.setVoided(false);
            adjustment.setUuid(UUID.randomUUID().toString());
            mapped.add(adjustment);
        }

        if (instance.getAdjustments() == null) {
            instance.setAdjustments(new ArrayList<>(mapped.size()));
        }

        List<BillLineItemAdjustment> existingTaxes = new ArrayList<>();
        List<BillLineItemAdjustment> nonTaxes = new ArrayList<>();
        for (BillLineItemAdjustment existing : instance.getAdjustments()) {
            if (existing != null && "TAX".equalsIgnoreCase(existing.getAdjustmentType())) {
                existingTaxes.add(existing);
            } else if (existing != null) {
                nonTaxes.add(existing);
            }
        }

        BaseRestDataResource.syncCollection(existingTaxes, mapped);
        instance.getAdjustments().clear();
        instance.getAdjustments().addAll(nonTaxes);
        instance.getAdjustments().addAll(existingTaxes);

        for (BillLineItemAdjustment adjustment : instance.getAdjustments()) {
            adjustment.setBillLineItem(instance);
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid decimal value for discount", ex);
        }
    }

    private String getStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String extractUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map) {
            Object uuid = ((Map<?, ?>) value).get("uuid");
            return uuid == null ? null : uuid.toString();
        }
        return null;
    }

    private org.openmrs.Provider resolveProvider(Object value) {
        String uuid = getStringValue(value);
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        org.openmrs.Provider provider = Context.getProviderService().getProviderByUuid(uuid);
        if (provider == null) {
            throw new IllegalArgumentException("Invalid sponsor provider UUID for discount");
        }
        return provider;
    }

    @Override
    protected void delete(BillLineItem delegate, String reason, RequestContext context) throws ResponseException {
        checkNoAllocations(delegate);
        super.delete(delegate, reason, context);
    }

    @Override
    public void purge(BillLineItem delegate, RequestContext context) throws ResponseException {
        checkNoAllocations(delegate);
        super.purge(delegate, context);
    }

    private void checkNoAllocations(BillLineItem lineItem) {
        if (lineItem.getAllocations() != null) {
            for (LinePaymentAllocation allocation : lineItem.getAllocations()) {
                if (allocation != null && !Boolean.TRUE.equals(allocation.getVoided())) {
                    throw new IllegalStateException(
                        "Cannot delete line item '" + lineItem.getUuid()
                        + "' because it has payment allocations. Please remove the associated payment(s) first.");
                }
            }
        }
    }

    @Override
    public BillLineItem getByUniqueId(String uuid) {
        return getService().getByUuid(uuid);
    }

    @Override
    public BillLineItem newDelegate() {
        return new BillLineItem();
    }

    @Override
    protected AlreadyPaged<BillLineItem> doSearch(RequestContext context) {
        String orderUuid = context.getRequest().getParameter("orderUuid");
        Order order = Strings.isNotEmpty(orderUuid) ? Context.getOrderService().getOrderByUuid(orderUuid) : null;
        if (order != null) {
            BillLineItem billItemSearch = new BillLineItem();
            billItemSearch.setOrder(order);
            BillLineItemService service = Context.getService(BillLineItemService.class);
            List<BillLineItem> result = service.fetchBillItemByOrder(new BillItemSearch(billItemSearch, false));
            return new AlreadyPaged<>(context, result, false);
        }
        return super.doSearch(context);
    }

    @Override
    public Class<IEntityDataService<BillLineItem>> getServiceClass() {
        return (Class<IEntityDataService<BillLineItem>>) (Object) BillLineItemService.class;
    }
}
