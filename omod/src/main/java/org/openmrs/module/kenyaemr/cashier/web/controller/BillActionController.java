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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for bill actions like close, reopen, and status synchronization.
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1)
public class BillActionController extends BaseRestController {

	@RequestMapping(value = "/kenyaemr-cashier/bill/{billUuid}/close", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> closeBill(@PathVariable("billUuid") String billUuid,
	        @RequestBody Map<String, Object> requestBody) {
		try {
			String reason = (String) requestBody.get("reason");
			if (reason == null || reason.trim().isEmpty()) {
				return error("Close reason is required", HttpStatus.BAD_REQUEST);
			}

			IBillService service = Context.getService(IBillService.class);
			Bill bill = service.getByUuid(billUuid);
			if (bill == null) {
				return error("Bill not found with UUID: " + billUuid, HttpStatus.NOT_FOUND);
			}

			Bill closedBill = service.closeBill(bill, reason);
			return new ResponseEntity<Map<String, Object>>(buildBillResponse(closedBill, "Bill closed successfully"),
			        HttpStatus.OK);
		} catch (IllegalStateException e) {
			return error(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return error("An error occurred while closing the bill: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/kenyaemr-cashier/bill/{billUuid}/reopen", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> reopenBill(@PathVariable("billUuid") String billUuid) {
		try {
			IBillService service = Context.getService(IBillService.class);
			Bill bill = service.getByUuid(billUuid);
			if (bill == null) {
				return error("Bill not found with UUID: " + billUuid, HttpStatus.NOT_FOUND);
			}

			Bill reopenedBill = service.reopenBill(bill);
			return new ResponseEntity<Map<String, Object>>(buildBillResponse(reopenedBill, "Bill reopened successfully"),
			        HttpStatus.OK);
		} catch (IllegalStateException e) {
			return error(e.getMessage(), HttpStatus.BAD_REQUEST);
		} catch (Exception e) {
			return error("An error occurred while reopening the bill: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/cashier/bill/sync-status/{billUuid}", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> syncBillStatus(@PathVariable("billUuid") String billUuid) {
		try {
			IBillService service = Context.getService(IBillService.class);
			Bill syncedBill = service.syncBillStatus(billUuid);
			return new ResponseEntity<Map<String, Object>>(
			        buildBillResponse(syncedBill, "Bill status synchronized successfully"), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			return error(e.getMessage(), HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			return error("An error occurred while synchronizing bill status: " + e.getMessage(),
			    HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
		Map<String, Object> error = new HashMap<String, Object>();
		error.put("error", message);
		return new ResponseEntity<Map<String, Object>>(error, status);
	}

	private Map<String, Object> buildBillResponse(Bill bill, String message) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("uuid", bill.getUuid());
		response.put("receiptNumber", bill.getReceiptNumber());
		response.put("status", bill.getStatus());
		response.put("closed", bill.isClosed());
		response.put("closeReason", bill.getCloseReason());
		response.put("closedBy", bill.getClosedBy() != null ? bill.getClosedBy().getUuid() : null);
		response.put("dateClosed", bill.getDateClosed());
		response.put("total", bill.getTotal());
		response.put("totalDiscount", bill.getTotalDiscount());
		response.put("balance", bill.getBalance());
		response.put("message", message);
		return response;
	}
}
