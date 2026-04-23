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
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for bill actions like close and reopen.
 * This controller provides action endpoints for bill operations.
 * 
 * Endpoints:
 * - POST /rest/v1/kenyaemr-cashier/bill/{billUuid}/close
 * - POST /rest/v1/kenyaemr-cashier/bill/{billUuid}/reopen
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
        response.put("balance", bill.getBalance());
        response.put("message", message);
        return response;
    }
}
