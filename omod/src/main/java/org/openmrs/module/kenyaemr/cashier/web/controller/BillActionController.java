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
package org.openmrs.module.kenyaemr.cashier.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.Provider;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.BillableServiceTax;
import org.openmrs.module.kenyaemr.cashier.api.model.CashierTaxType;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for bill actions like close and reopen.
 * This controller provides action endpoints for bill operations.
 * 
 * Endpoints:
     * - POST /rest/v1/kenyaemr-cashier/bill/{billUuid}/close
     * - POST /rest/v1/kenyaemr-cashier/bill/{billUuid}/reopen
     * - POST /rest/v1/kenyaemr-cashier/bill/{billUuid}/additional-discount
     * - POST /rest/v1/cashier/bill/sync-status/{billUuid}
     */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1)
public class BillActionController extends BaseRestController {
    /**
     * Closes a bill manually, preventing new items from being added.
     * 
     * @param billUuid The UUID of the bill to close
     * @param requestBody The request body containing the close reason
     * @return The updated bill information
     */
    @RequestMapping(value = "/kenyaemr-cashier/bill/{billUuid}/close", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> closeBill(
            @PathVariable("billUuid") String billUuid,
            @RequestBody Map<String, Object> requestBody) {
        
        try {
            String reason = (String) requestBody.get("reason");
            
            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Close reason is required");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            
            IBillService service = Context.getService(IBillService.class);
            Bill bill = service.getByUuid(billUuid);
            
            if (bill == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bill not found with UUID: " + billUuid);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            Bill closedBill = service.closeBill(bill, reason);

            return new ResponseEntity<>(buildBillResponse(closedBill, "Bill closed successfully"), HttpStatus.OK);
            
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while closing the bill: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reopens a closed bill, allowing new items to be added.
     * 
     * @param billUuid The UUID of the bill to reopen
     * @return The updated bill information
     */
    @RequestMapping(value = "/kenyaemr-cashier/bill/{billUuid}/reopen", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reopenBill(
            @PathVariable("billUuid") String billUuid) {
        
        try {
            IBillService service = Context.getService(IBillService.class);
            Bill bill = service.getByUuid(billUuid);
            
            if (bill == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bill not found with UUID: " + billUuid);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            Bill reopenedBill = service.reopenBill(bill);

            return new ResponseEntity<>(buildBillResponse(reopenedBill, "Bill reopened successfully"), HttpStatus.OK);
            
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while reopening the bill: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
	    }

    @RequestMapping(value = "/kenyaemr-cashier/bill/{billUuid}/additional-discount", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAdditionalDiscount(
            @PathVariable("billUuid") String billUuid,
            @RequestBody Map<String, Object> requestBody) {

        try {
            IBillService service = Context.getService(IBillService.class);
            Bill bill = service.getByUuid(billUuid);

            if (bill == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bill not found with UUID: " + billUuid);
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }

            if (Boolean.TRUE.equals(bill.isClosed())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Discounts cannot be updated on a closed bill.");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }

            BigDecimal additionalDiscount = parseDiscountsAmount(requestBody);
            Provider sponsor = resolveProvider(requestBody.get("sponsor"));
            String comment = parseComment(requestBody.get("comment"));
            applyBulkDiscounts(bill, additionalDiscount, sponsor, comment);
            Bill updatedBill = service.save(bill);

            return new ResponseEntity<>(buildBillResponse(updatedBill, "Discounts updated successfully"),
                    HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while updating discounts: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	    /**
	     * Alias endpoint for bill status synchronization. The documented REST resource path is
     * POST /rest/v1/cashier/bill/{billUuid}/sync-status, but this alias is kept for callers
     * using the explicit action-style URL.
     *
     * @param billUuid The UUID of the bill to resynchronize
     * @return The updated bill information
     */
    @RequestMapping(value = "/cashier/bill/sync-status/{billUuid}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> syncBillStatus(
            @PathVariable("billUuid") String billUuid) {

        try {
            IBillService service = Context.getService(IBillService.class);
            Bill syncedBill = service.syncBillStatus(billUuid);

            return new ResponseEntity<>(buildBillResponse(syncedBill, "Bill status synchronized successfully"),
                    HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while synchronizing bill status: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> buildBillResponse(Bill bill, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("uuid", bill.getUuid());
        response.put("receiptNumber", bill.getReceiptNumber());
        response.put("status", bill.getStatus());
        response.put("closed", bill.isClosed());
        response.put("closeReason", bill.getCloseReason());
        response.put("closedBy", bill.getClosedBy() != null ? bill.getClosedBy().getUuid() : null);
	        response.put("dateClosed", bill.getDateClosed());
	        response.put("total", bill.getTotal());
	        response.put("totalDiscount", bill.getTotalDiscount());
	        response.put("additionalDiscount", BigDecimal.ZERO);
	        response.put("balance", bill.getBalance());
	        response.put("message", message);
	        return response;
	    }

    private BigDecimal parseDiscountsAmount(Map<String, Object> requestBody) {
        Object value = requestBody.get("discounts");
        if (value == null) {
            value = requestBody.get("amount");
        }
        if (value == null) {
            value = requestBody.get("additionalDiscount");
        }
        if (value == null) {
            throw new IllegalArgumentException("Discounts amount is required.");
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Discounts amount must be a valid decimal amount.");
        }
    }

    private Provider resolveProvider(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        Provider provider = Context.getProviderService().getProviderByUuid(String.valueOf(value).trim());
        if (provider == null) {
            throw new IllegalArgumentException("Invalid sponsor provider UUID for discounts.");
        }
        return provider;
    }

    private String parseComment(Object value) {
        if (value == null) {
            return null;
        }
        String comment = String.valueOf(value).trim();
        return comment.isEmpty() ? null : comment;
    }

    private void applyBulkDiscounts(Bill bill, BigDecimal requestedAmount, Provider sponsor, String comment) {
        BigDecimal normalizedAmount = requestedAmount == null ? BigDecimal.ZERO : requestedAmount;
        if (normalizedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Discounts amount cannot be negative.");
        }

        if (normalizedAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal eligibleAmount = calculateReplacementEligibleAmount(bill);
            if (normalizedAmount.compareTo(eligibleAmount) > 0) {
                throw new IllegalArgumentException("Discounts amount cannot exceed eligible unpaid line item amount.");
            }
        }

        recalculateBillLines(bill);
        bill.setAdditionalDiscount(BigDecimal.ZERO);

        if (normalizedAmount.compareTo(BigDecimal.ZERO) == 0) {
            clearAllLineDiscounts(bill);
            recalculateBillLines(bill);
            bill.synchronizeBillStatus();
            return;
        }

        clearEligibleLineDiscounts(bill);
        recalculateBillLines(bill);
        allocateBulkDiscounts(bill, normalizedAmount, sponsor, comment);

        recalculateBillLines(bill);
        bill.synchronizeBillStatus();
    }

    private BigDecimal calculateReplacementEligibleAmount(Bill bill) {
        BigDecimal total = BigDecimal.ZERO;
        for (BillLineItem lineItem : getSortedEligibleLineItems(bill)) {
            BigDecimal currentDiscount = calculateDiscountAmount(lineItem.getAdjustments());
            total = total.add(lineItem.getRemainingAmount()).add(currentDiscount);
        }
        return total;
    }

    private void clearEligibleLineDiscounts(Bill bill) {
        for (BillLineItem lineItem : getSortedEligibleLineItems(bill)) {
            removeActiveDiscounts(lineItem);
        }
    }

    private void clearAllLineDiscounts(Bill bill) {
        if (bill == null || bill.getLineItems() == null) {
            return;
        }
        for (BillLineItem lineItem : bill.getLineItems()) {
            if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
                continue;
            }
            removeActiveDiscounts(lineItem);
        }
    }

    private void removeActiveDiscounts(BillLineItem lineItem) {
        if (lineItem == null || lineItem.getAdjustments() == null) {
            return;
        }

        Iterator<BillLineItemAdjustment> iterator = lineItem.getAdjustments().iterator();
        while (iterator.hasNext()) {
            BillLineItemAdjustment existing = iterator.next();
            if (existing != null && !Boolean.TRUE.equals(existing.getVoided())
                    && "DISCOUNT".equalsIgnoreCase(existing.getAdjustmentType())) {
                iterator.remove();
            }
        }
    }

    private void allocateBulkDiscounts(Bill bill, BigDecimal amount, Provider sponsor, String comment) {
        BigDecimal remaining = amount;
        for (BillLineItem lineItem : getSortedEligibleLineItems(bill)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal lineOutstanding = lineItem.getRemainingAmount();
            if (lineOutstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocatedAmount = remaining.min(lineOutstanding);
            lineItem.addAdjustment(createLineDiscount(lineItem, allocatedAmount, lineOutstanding, sponsor, comment));
            recalculateTaxes(lineItem);
            lineItem.synchronizePaymentStatus();
            remaining = remaining.subtract(allocatedAmount);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Discounts amount cannot exceed eligible unpaid line item amount.");
        }
    }

    private BillLineItemAdjustment createLineDiscount(BillLineItem lineItem, BigDecimal amount, BigDecimal baseAmount,
            Provider sponsor, String comment) {
        BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
        adjustment.setAdjustmentType("DISCOUNT");
        adjustment.setAmount(amount);
        adjustment.setBaseAmount(baseAmount);
        adjustment.setDescription(comment);
        adjustment.setDiscountSponsor(sponsor);
        adjustment.setBillLineItem(lineItem);
        adjustment.setCreator(Context.getAuthenticatedUser());
        adjustment.setDateCreated(new Date());
        adjustment.setVoided(false);
        adjustment.setUuid(UUID.randomUUID().toString());
        return adjustment;
    }

    private List<BillLineItem> getSortedEligibleLineItems(Bill bill) {
        List<BillLineItem> eligible = new ArrayList<BillLineItem>();
        if (bill == null || bill.getLineItems() == null) {
            return eligible;
        }

        for (BillLineItem lineItem : bill.getLineItems()) {
            if (isEligibleForAdditionalDiscount(lineItem)) {
                eligible.add(lineItem);
            }
        }

        Collections.sort(eligible, new Comparator<BillLineItem>() {
            @Override
            public int compare(BillLineItem first, BillLineItem second) {
                int orderCompare = compareNullable(first.getLineItemOrder(), second.getLineItemOrder());
                if (orderCompare != 0) {
                    return orderCompare;
                }
                int idCompare = compareNullable(first.getId(), second.getId());
                if (idCompare != 0) {
                    return idCompare;
                }
                String firstUuid = first.getUuid() == null ? "" : first.getUuid();
                String secondUuid = second.getUuid() == null ? "" : second.getUuid();
                return firstUuid.compareTo(secondUuid);
            }
        });
        return eligible;
    }

    private int compareNullable(Integer first, Integer second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return 1;
        }
        if (second == null) {
            return -1;
        }
        return first.compareTo(second);
    }

    private boolean isEligibleForAdditionalDiscount(BillLineItem lineItem) {
        if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
            return false;
        }
        return lineItem.getPaymentStatus() == BillStatus.PENDING || lineItem.getPaymentStatus() == BillStatus.POSTED;
    }

    private void recalculateBillLines(Bill bill) {
        if (bill == null || bill.getLineItems() == null) {
            return;
        }
        for (BillLineItem lineItem : bill.getLineItems()) {
            if (lineItem == null || Boolean.TRUE.equals(lineItem.getVoided())) {
                continue;
            }
            recalculateTaxes(lineItem);
            lineItem.synchronizePaymentStatus();
        }
    }

    private void recalculateTaxes(BillLineItem lineItem) {
        if (lineItem.getPrice() == null || lineItem.getQuantity() == null) {
            return;
        }
        if (lineItem.getAdjustments() == null) {
            lineItem.setAdjustments(new ArrayList<BillLineItemAdjustment>());
        }

        Iterator<BillLineItemAdjustment> iterator = lineItem.getAdjustments().iterator();
        while (iterator.hasNext()) {
            BillLineItemAdjustment existing = iterator.next();
            if (existing != null && "TAX".equalsIgnoreCase(existing.getAdjustmentType())) {
                iterator.remove();
            }
        }

        BigDecimal taxableAmount = lineItem.getSubTotal().subtract(calculateDiscountAmount(lineItem.getAdjustments()));
        if (taxableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (lineItem.getBillableService() == null || lineItem.getBillableService().getServiceTaxes() == null) {
            return;
        }

        for (BillableServiceTax serviceTax : lineItem.getBillableService().getServiceTaxes()) {
            if (serviceTax == null || Boolean.TRUE.equals(serviceTax.getVoided())) {
                continue;
            }
            CashierTaxType taxType = serviceTax.getTaxType();
            BigDecimal rate = serviceTax.getOverrideRate() != null ? serviceTax.getOverrideRate()
                    : (taxType == null ? null : taxType.getRate());
            if (rate == null) {
                continue;
            }
            BillLineItemAdjustment tax = new BillLineItemAdjustment();
            tax.setAdjustmentType("TAX");
            tax.setTaxType(taxType);
            tax.setRate(rate);
            tax.setBaseAmount(taxableAmount);
            tax.setAmount(taxableAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP));
            tax.setBillLineItem(lineItem);
            tax.setCreator(Context.getAuthenticatedUser());
            tax.setDateCreated(new Date());
            tax.setVoided(false);
            tax.setUuid(UUID.randomUUID().toString());
            lineItem.getAdjustments().add(tax);
        }
    }

    private BigDecimal calculateDiscountAmount(List<BillLineItemAdjustment> adjustments) {
        BigDecimal total = BigDecimal.ZERO;
        if (adjustments == null) {
            return total;
        }
        for (BillLineItemAdjustment adjustment : adjustments) {
            if (adjustment == null || Boolean.TRUE.equals(adjustment.getVoided())) {
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
                total = total.add(amount);
            }
        }
        return total;
    }

	}
