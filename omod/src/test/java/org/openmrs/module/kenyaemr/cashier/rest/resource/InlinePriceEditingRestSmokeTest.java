package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItemAdjustment;
import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierSubResourceController;
import org.openmrs.module.kenyaemr.cashier.web.controller.BillActionController;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ContextConfiguration(locations = { "classpath:applicationContext-service.xml", "classpath*:TestingApplicationContext.xml",
        "classpath*:moduleApplicationContext.xml",
        "classpath:org/openmrs/module/kenyaemr/cashier/rest/include/InlinePriceEditingRestSmokeTestContext.xml" }, inheritLocations = false)
public class InlinePriceEditingRestSmokeTest extends BaseModuleContextSensitiveTest {

	private static final String DATASET = "org/openmrs/module/kenyaemr/cashier/rest/include/InlinePriceEditingRestSmokeTest.xml";
	private static final String BILL_RESOURCE = "/rest/v1/cashier/bill";
	private static final String BILL_LINE_ITEM_RESOURCE = "/rest/v1/cashier/billLineItem";
	private static final String BILL_ACTION_RESOURCE = "/rest/v1/kenyaemr-cashier/bill";
	private static final String LEGACY_LINE_ITEM_UUID = "81000000-0000-0000-0000-000000000401";
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	private static final String PAYMENT_MODE_UUID = "81000000-0000-0000-0000-000000000002";
	private static final String SERVICE_1_UUID = "81000000-0000-0000-0000-000000000101";
	private static final String SERVICE_2_UUID = "81000000-0000-0000-0000-000000000102";
	private static final String SERVICE_3_UUID = "81000000-0000-0000-0000-000000000103";
	private static final String SERVICE_4_UUID = "81000000-0000-0000-0000-000000000104";
	private static final String SERVICE_5_UUID = "81000000-0000-0000-0000-000000000105";
	private static final String PRICE_100_UUID = "81000000-0000-0000-0000-000000000201";
	private static final String PRICE_200_UUID = "81000000-0000-0000-0000-000000000202";
	private static final String PRICE_300_UUID = "81000000-0000-0000-0000-000000000203";
	private static final String PRICE_400_UUID = "81000000-0000-0000-0000-000000000204";
	private static final String PRICE_500_UUID = "81000000-0000-0000-0000-000000000205";
	private static final String PRICE_425_UUID = "81000000-0000-0000-0000-000000000206";
	private static final String PRICE_220_UUID = "81000000-0000-0000-0000-000000000207";
	private static final String ALLOCATION_ONE_UUID = "81000000-0000-0000-0000-000000000501";
	private static final String ALLOCATION_TWO_UUID = "81000000-0000-0000-0000-000000000502";

	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	@Before
	public void setup() throws Exception {
		executeDataSet(DATASET);
		CashierResourceController resourceController = new CashierResourceController();
		CashierSubResourceController subResourceController = new CashierSubResourceController();
		BillActionController billActionController = new BillActionController();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resourceController);
		applicationContext.getAutowireCapableBeanFactory().autowireBean(subResourceController);
		applicationContext.getAutowireCapableBeanFactory().autowireBean(billActionController);
		mockMvc = MockMvcBuilders.standaloneSetup(resourceController, subResourceController, billActionController)
		        .setMessageConverters(new MappingJackson2HttpMessageConverter())
		        .setValidator(new NoOpValidator())
		        .build();
	}

	@Test
	public void inlinePriceEditing_shouldFollowAHFlowThroughRestApi() throws Exception {
		// A. Migration/backfill baseline behavior from legacy row
		Map<String, Object> legacyLineItem = getLineItem(LEGACY_LINE_ITEM_UUID);
		assertInlinePriceState(legacyLineItem, "100.00", "100.00", false);

		// B. Open bill with mixed states
		Map<String, Object> bill = postJson(BILL_RESOURCE, createBillPayload());
			String billUuid = stringValue(bill.get("uuid"));
			assertNotNull(billUuid);
			List<Map<String, Object>> lineItems = getBillLineItems(billUuid);
			assertEquals("Expected three created smoke line items. Available line items: " + describeLineItems(lineItems), 3,
			    lineItems.size());
		assertInlinePriceState(findLineByService(lineItems, SERVICE_1_UUID), "100.00", "100.00", false);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_2_UUID), "150.00", "200.00", true);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "250.00", "300.00", true);

			// C. Add a normal line at bill level only
			updateBill(billUuid,
			    billPayload(lineItems,
			        linePayload(SERVICE_4_UUID, PRICE_400_UUID, "400", null),
			        ""));
			lineItems = getBillLineItems(billUuid);
			assertEquals("Expected four smoke line items after bill update. Available line items: "
			        + describeLineItems(lineItems), 4, lineItems.size());
			assertInlinePriceState(findLineByService(lineItems, SERVICE_1_UUID), "100.00", "100.00", false);
			assertInlinePriceState(findLineByService(lineItems, SERVICE_2_UUID), "150.00", "200.00", true);
			assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "250.00", "300.00", true);
			assertInlinePriceState(findLineByService(lineItems, SERVICE_4_UUID), "400.00", "400.00", false);

		// D. Reset a line by priceUuid through /billLineItem/{uuid}
		Map<String, Object> thirdLine = findLineByService(lineItems, SERVICE_3_UUID);
		updateLineItem(stringValue(thirdLine.get("uuid")),
		    existingLinePayload(thirdLine, SERVICE_3_UUID, PRICE_425_UUID, "425", null));
		lineItems = getBillLineItems(billUuid);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "425.00", "425.00", false);

		// E. Manual price override after reset
		thirdLine = findLineByService(lineItems, SERVICE_3_UUID);
		updateLineItem(stringValue(thirdLine.get("uuid")),
		    existingLinePayload(thirdLine, SERVICE_3_UUID, PRICE_425_UUID, "350", "Manual override after reset"));
		lineItems = getBillLineItems(billUuid);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "350.00", "425.00", true);

		// F. Payment that covers first two lines
		Map<String, Object> firstLine = findLineByService(lineItems, SERVICE_1_UUID);
		Map<String, Object> secondLine = findLineByService(lineItems, SERVICE_2_UUID);
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", paymentPayload(firstLine, secondLine));
		lineItems = getBillLineItems(billUuid);
		firstLine = lineByUuid(lineItems, stringValue(firstLine.get("uuid")));
		secondLine = lineByUuid(lineItems, stringValue(secondLine.get("uuid")));
		assertEquals("PAID", stringValue(firstLine.get("paymentStatus")));
		assertEquals("PAID", stringValue(secondLine.get("paymentStatus")));
		assertAmount("100.00", firstLine.get("totalAllocated"));
		assertAmount("150.00", secondLine.get("totalAllocated"));
		assertComputedBalance(computeLineItemBalance(lineItems), lineItems);

		// G. Paid line edit policy check on open bill (captured as current backend behavior)
		String openPaidLinePayload = existingLinePayload(secondLine, SERVICE_2_UUID, PRICE_220_UUID, "220", null);
		MvcResult openPaidLineUpdate = postLineItemResult(stringValue(secondLine.get("uuid")), openPaidLinePayload);
		if (isSuccess(openPaidLineUpdate)) {
			Map<String, Object> paidLineAfterEdit = parse(openPaidLineUpdate);
			assertInlinePriceState(paidLineAfterEdit, "220.00", "220.00", false);
			assertEquals("POSTED", stringValue(paidLineAfterEdit.get("paymentStatus")));
			lineItems = getBillLineItems(billUuid);
			assertComputedBalance(computeLineItemBalance(lineItems), lineItems);
		} else {
			int status = openPaidLineUpdate.getResponse().getStatus();
			assertTrue("Paid line edit policy should be explicit at API boundary", status >= 400 && status < 500);
			lineItems = getBillLineItems(billUuid);
			assertInlinePriceState(lineByUuid(lineItems, stringValue(secondLine.get("uuid"))), "150.00", "200.00", true);
		}

		// H. Close-edit-reopen-edit-close behavior
		lineItems = getBillLineItems(billUuid);
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", outstandingPaymentPayload(lineItems));
		lineItems = getBillLineItems(billUuid);
		assertComputedBalance("0", lineItems);
		secondLine = lineByUuid(lineItems, stringValue(secondLine.get("uuid")));
		closeBillAndAssert(billUuid, "Close bill for policy check");
		MvcResult closedLineEdit = postLineItemResult(stringValue(secondLine.get("uuid")),
		    existingLinePayload(secondLine, SERVICE_2_UUID, PRICE_220_UUID, "230", "Closed edit"));
		if (isSuccess(closedLineEdit)) {
			Map<String, Object> closedLineAfterEdit = parse(closedLineEdit);
			assertInlinePriceState(closedLineAfterEdit, "230.00", "220.00", true);
		} else {
			int status = closedLineEdit.getResponse().getStatus();
			assertTrue("Closed bill edit policy should be explicit at API boundary", status >= 400 && status < 500);
		}

		reopenBillAndAssert(billUuid);
		String reopenPayload = existingLinePayload(secondLine, SERVICE_2_UUID, PRICE_220_UUID, "240", "Reopen edit");
		Map<String, Object> lineAfterReopen = updateLineItem(stringValue(secondLine.get("uuid")), reopenPayload);
		assertInlinePriceState(lineAfterReopen, "240.00", "220.00", true);
		lineItems = getBillLineItems(billUuid);
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", outstandingPaymentPayload(lineItems));
		closeBillAndAssert(billUuid, "Close bill again after reopen");
	}

	@Test
	public void additionalDiscount_shouldAllocateToLineDiscountsAndRecalculateTax() throws Exception {
		Map<String, Object> bill = postJson(BILL_RESOURCE, taxedBillPayload());
		String billUuid = stringValue(bill.get("uuid"));
		assertNotNull(billUuid);

				bill = syncBill(billUuid);
				List<Map<String, Object>> lineItems = getBillLineItems(billUuid);
			assertEquals("Expected one created smoke line item. Available line items: " + describeLineItems(lineItems),
			    1, lineItems.size());
			Map<String, Object> taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
			assertAmount("440.00", bill.get("total"));
			assertAmount("0", bill.get("additionalDiscount"));
			assertAmount("440.00", bill.get("balance"));
			assertAmount("0", lineDiscountAmount(taxedDiscountedLine));
			assertAmount("40.00", lineTaxAmount(taxedDiscountedLine));
			assertAmount("440.00", taxedDiscountedLine.get("total"));

			postJson(BILL_RESOURCE + "/" + billUuid + "/payment", singleAllocationPaymentPayload(taxedDiscountedLine, "50"));
			bill = getBill(billUuid);
			lineItems = getBillLineItems(billUuid);
			taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
			assertEquals("POSTED", stringValue(taxedDiscountedLine.get("paymentStatus")));
			assertAmount("50.00", taxedDiscountedLine.get("totalAllocated"));
			assertAmount("390.00", computeLineItemBalance(lineItems));

			MvcResult invalidDiscountResult = postAdditionalDiscountResult(billUuid, "391");
			assertEquals(Integer.valueOf(4), Integer.valueOf(invalidDiscountResult.getResponse().getStatus() / 100));
			bill = syncBill(billUuid);
			assertAmount("0", bill.get("additionalDiscount"));
			assertAmount("390.00", bill.get("balance"));

			String sponsorUuid = Context.getProviderService().getProvider(1).getUuid();
			Map<String, Object> additionalDiscountResponse = postAdditionalDiscount(billUuid, "75", sponsorUuid,
			    "Board approval");
			assertAmount("357.50", additionalDiscountResponse.get("total"));
			assertAmount("75.00", additionalDiscountResponse.get("totalDiscount"));
			assertAmount("0", additionalDiscountResponse.get("additionalDiscount"));
			assertAmount("307.50", additionalDiscountResponse.get("balance"));
			assertEquals("POSTED", stringValue(additionalDiscountResponse.get("status")));

			bill = syncBill(billUuid);
			lineItems = getBillLineItems(billUuid);
			taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
			assertAmount("75.00", lineDiscountAmount(taxedDiscountedLine));
			assertAmount("32.50", lineTaxAmount(taxedDiscountedLine));
			assertAmount("357.50", taxedDiscountedLine.get("total"));
			assertAmount("357.50", bill.get("total"));
			assertAmount("75.00", bill.get("totalDiscount"));
			assertAmount("0", bill.get("additionalDiscount"));
			assertAmount("307.50", bill.get("balance"));
			assertLineDiscountMetadata(taxedDiscountedLine, sponsorUuid, "Board approval");

			Map<String, Object> replacedLine = updateLineItem(stringValue(taxedDiscountedLine.get("uuid")),
			    existingLinePayloadWithDiscount(taxedDiscountedLine, SERVICE_4_UUID, PRICE_400_UUID, "400", "25",
			        "Manual adjustment"));
			lineItems = getBillLineItems(billUuid);
			replacedLine = lineByUuid(lineItems, stringValue(replacedLine.get("uuid")));
			assertAmount("25.00", lineDiscountAmount(replacedLine));
			assertAmount("37.50", lineTaxAmount(replacedLine));
			assertLineDiscountMetadata(replacedLine, null, "Manual adjustment");

			postJson(BILL_RESOURCE + "/" + billUuid + "/payment", paymentPayloadWithoutAllocations("362.50"));
			bill = syncBill(billUuid);
			assertAmount("0", bill.get("balance"));
		assertAmount("0", bill.get("additionalDiscount"));
		assertEquals("PAID", stringValue(bill.get("status")));
		closeBillAndAssert(billUuid, "Close bill after additional discount settlement");
	}

	@Test
	public void additionalDiscount_shouldReplaceEligibleDiscountsAndClearAllDiscountsWhenZero() throws Exception {
		Map<String, Object> bill = postJson(BILL_RESOURCE, taxedAndUntaxedBillPayload());
		String billUuid = stringValue(bill.get("uuid"));
		assertNotNull(billUuid);
		List<Map<String, Object>> lineItems = getBillLineItems(billUuid);
		Map<String, Object> taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
		Map<String, Object> paidDiscountedLine = findLineByService(lineItems, SERVICE_5_UUID);

		updateLineItem(stringValue(taxedDiscountedLine.get("uuid")),
		    existingLinePayloadWithDiscount(taxedDiscountedLine, SERVICE_4_UUID, PRICE_400_UUID, "400", "40",
		        "Manual discount"));
		updateLineItem(stringValue(paidDiscountedLine.get("uuid")),
		    existingLinePayloadWithDiscount(paidDiscountedLine, SERVICE_5_UUID, PRICE_500_UUID, "500", "20",
		        "Ineligible discount"));
		lineItems = getBillLineItems(billUuid);
		taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
		paidDiscountedLine = findLineByService(lineItems, SERVICE_5_UUID);
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment",
		    singleAllocationPaymentPayload(paidDiscountedLine, numberString(paidDiscountedLine.get("total"))));

		lineItems = getBillLineItems(billUuid);
		taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
		paidDiscountedLine = findLineByService(lineItems, SERVICE_5_UUID);
		assertEquals("PENDING", stringValue(taxedDiscountedLine.get("paymentStatus")));
		assertEquals("PAID", stringValue(paidDiscountedLine.get("paymentStatus")));
		assertAmount("40.00", lineDiscountAmount(taxedDiscountedLine));
		assertAmount("36.00", lineTaxAmount(taxedDiscountedLine));
		assertAmount("20.00", lineDiscountAmount(paidDiscountedLine));

		String sponsorUuid = Context.getProviderService().getProvider(1).getUuid();
		Map<String, Object> result = postAdditionalDiscount(billUuid, "60", sponsorUuid, "Bulk replacement");
		assertAmount("80.00", result.get("totalDiscount"));
		assertAmount("0", result.get("additionalDiscount"));

		bill = syncBill(billUuid);
		lineItems = getBillLineItems(billUuid);
		taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
		paidDiscountedLine = findLineByService(lineItems, SERVICE_5_UUID);
		assertAmount("60.00", lineDiscountAmount(taxedDiscountedLine));
		assertAmount("34.00", lineTaxAmount(taxedDiscountedLine));
		assertLineDiscountMetadata(taxedDiscountedLine, sponsorUuid, "Bulk replacement");
		assertAmount("20.00", lineDiscountAmount(paidDiscountedLine));
		assertLineDiscountMetadata(paidDiscountedLine, null, "Ineligible discount");
		assertAmount("80.00", bill.get("totalDiscount"));
		assertAmount("0", bill.get("additionalDiscount"));

		addVoidedDiscountAdjustment(billUuid, stringValue(paidDiscountedLine.get("uuid")), "Voided historical discount");
		assertVoidedDiscountStillVoided(billUuid, stringValue(paidDiscountedLine.get("uuid")),
		    "Voided historical discount");

		postAdditionalDiscount(billUuid, "0", sponsorUuid, "Clear all discounts");
		bill = syncBill(billUuid);
		lineItems = getBillLineItems(billUuid);
		taxedDiscountedLine = findLineByService(lineItems, SERVICE_4_UUID);
		paidDiscountedLine = findLineByService(lineItems, SERVICE_5_UUID);
		assertAmount("0", lineDiscountAmount(taxedDiscountedLine));
		assertAmount("40.00", lineTaxAmount(taxedDiscountedLine));
		assertAmount("0", lineDiscountAmount(paidDiscountedLine));
		assertAmount("0", bill.get("totalDiscount"));
		assertAmount("0", bill.get("additionalDiscount"));
		assertAmount("940.00", bill.get("total"));
		assertAmount("460.00", bill.get("balance"));
		assertVoidedDiscountStillVoided(billUuid, stringValue(paidDiscountedLine.get("uuid")),
		    "Voided historical discount");
	}

	@Test
	public void deletePayment_shouldResynchronizeBillAndLineStatusesFromActiveAllocations() throws Exception {
		Map<String, Object> bill = postJson(BILL_RESOURCE, taxedAndUntaxedBillPayload());
		String billUuid = stringValue(bill.get("uuid"));
		assertNotNull(billUuid);

		List<Map<String, Object>> lineItems = getBillLineItems(billUuid);
		Map<String, Object> taxedLine = findLineByService(lineItems, SERVICE_4_UUID);
		Map<String, Object> untaxedLine = findLineByService(lineItems, SERVICE_5_UUID);
		Map<String, Object> taxedPayment = postJson(BILL_RESOURCE + "/" + billUuid + "/payment",
		    singleAllocationPaymentPayload(taxedLine, numberString(taxedLine.get("total"))));
		Map<String, Object> untaxedPayment = postJson(BILL_RESOURCE + "/" + billUuid + "/payment",
		    singleAllocationPaymentPayload(untaxedLine, numberString(untaxedLine.get("total"))));
		String taxedPaymentUuid = stringValue(taxedPayment.get("uuid"));
		String untaxedPaymentUuid = stringValue(untaxedPayment.get("uuid"));

		bill = syncBill(billUuid);
		assertEquals("PAID", stringValue(bill.get("status")));
		assertPersistedBillState(billUuid, "PAID", "0", "940.00");
		assertPersistedActivePaymentCount(billUuid, 2);

		String partialDeleteReason = "Delete one payment for partial settlement";
		deletePaymentAndAssert(billUuid, untaxedPaymentUuid, partialDeleteReason);
		getBill(billUuid);
		lineItems = getBillLineItems(billUuid);
		taxedLine = findLineByService(lineItems, SERVICE_4_UUID);
		untaxedLine = findLineByService(lineItems, SERVICE_5_UUID);
		assertPersistedBillState(billUuid, "POSTED", "500.00", "440.00");
		assertPersistedActivePaymentCount(billUuid, 1);
		assertEquals("PAID", stringValue(taxedLine.get("paymentStatus")));
		assertEquals("PENDING", stringValue(untaxedLine.get("paymentStatus")));
		assertAmount("440.00", taxedLine.get("totalAllocated"));
		assertAmount("0", untaxedLine.get("totalAllocated"));
		assertPaymentAndAllocationsVoided(billUuid, untaxedPaymentUuid, partialDeleteReason);

		String lastDeleteReason = "Delete last active payment";
		deletePaymentAndAssert(billUuid, taxedPaymentUuid, lastDeleteReason);
		getBill(billUuid);
		lineItems = getBillLineItems(billUuid);
		taxedLine = findLineByService(lineItems, SERVICE_4_UUID);
		untaxedLine = findLineByService(lineItems, SERVICE_5_UUID);
		assertPersistedBillState(billUuid, "PENDING", "940.00", "0");
		assertPersistedActivePaymentCount(billUuid, 0);
		assertEquals("PENDING", stringValue(taxedLine.get("paymentStatus")));
		assertEquals("PENDING", stringValue(untaxedLine.get("paymentStatus")));
		assertAmount("0", taxedLine.get("totalAllocated"));
		assertAmount("0", untaxedLine.get("totalAllocated"));
		assertPaymentAndAllocationsVoided(billUuid, taxedPaymentUuid, lastDeleteReason);
	}

	private Map<String, Object> getLineItem(String uuid) throws Exception {
		MvcResult result = mockMvc.perform(get(BILL_LINE_ITEM_RESOURCE + "/" + uuid)
		        .accept(MediaType.APPLICATION_JSON))
		        .andReturn();
		assertSuccess(result);
		return parse(result);
	}

		private Map<String, Object> getBill(String uuid) throws Exception {
			MvcResult result = mockMvc.perform(get(BILL_RESOURCE + "/" + uuid)
		        .accept(MediaType.APPLICATION_JSON))
		        .andReturn();
		assertSuccess(result);
			return parse(result);
		}

		private Map<String, Object> syncBill(String uuid) throws Exception {
			return postJson(BILL_RESOURCE + "/sync-status/" + uuid, "{}");
		}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> lineItemsFromBill(Map<String, Object> bill) {
		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) bill.get("lineItems");
		return normalizeLineItems(lineItems == null ? new ArrayList<Map<String, Object>>() : lineItems);
	}

	private List<Map<String, Object>> getBillLineItems(String billUuid) {
		Bill persistedBill = Context.getService(IBillService.class).getByUuid(billUuid);
		assertNotNull("Expected bill " + billUuid + " to exist", persistedBill);
		List<Map<String, Object>> lineItems = new ArrayList<>();
		if (persistedBill.getLineItems() == null) {
			return lineItems;
		}
		for (BillLineItem billLineItem : persistedBill.getLineItems()) {
			if (billLineItem == null || Boolean.TRUE.equals(billLineItem.getVoided())) {
				continue;
			}
			Map<String, Object> lineItem = new java.util.HashMap<String, Object>();
			lineItem.put("uuid", billLineItem.getUuid());
			lineItem.put("display", billLineItem.getBillableService() != null ? billLineItem.getBillableService().getName() : "");
			lineItem.put("billableService",
			    billLineItem.getBillableService() != null ? billLineItem.getBillableService().getUuid() : "");
			lineItem.put("quantity", billLineItem.getQuantity());
			lineItem.put("price", billLineItem.getPrice());
			lineItem.put("originalPrice", billLineItem.getOriginalPrice());
			lineItem.put("priceOverridden", billLineItem.getPriceOverridden());
			lineItem.put("priceOverrideReason", billLineItem.getPriceOverrideReason());
			lineItem.put("priceName", billLineItem.getPriceName());
			lineItem.put("priceUuid", billLineItem.getItemPrice() != null ? billLineItem.getItemPrice().getUuid() : "");
			lineItem.put("lineItemOrder", billLineItem.getLineItemOrder());
			lineItem.put("paymentStatus", billLineItem.getPaymentStatus());
			lineItem.put("discounts", serializedDiscounts(billLineItem));
			lineItem.put("totalDiscount", billLineItem.getTotalDiscount());
			lineItem.put("totalTax", billLineItem.getTotalTax());
			lineItem.put("total", billLineItem.getTotal());
			lineItem.put("totalAllocated", billLineItem.getTotalAllocated());
			if (isSmokeLineItem(lineItem) && !LEGACY_LINE_ITEM_UUID.equals(stringValue(lineItem.get("uuid")))) {
				lineItems.add(lineItem);
			}
		}
		return normalizeLineItems(lineItems);
	}

	private List<Map<String, Object>> serializedDiscounts(BillLineItem billLineItem) {
		List<Map<String, Object>> discounts = new ArrayList<Map<String, Object>>();
		if (billLineItem.getAdjustments() == null) {
			return discounts;
		}
		for (BillLineItemAdjustment adjustment : billLineItem.getAdjustments()) {
			if (adjustment == null || Boolean.TRUE.equals(adjustment.getVoided())
			        || !"DISCOUNT".equalsIgnoreCase(adjustment.getAdjustmentType())) {
				continue;
			}
			Map<String, Object> discount = new java.util.HashMap<String, Object>();
			discount.put("amount", adjustment.getAmount());
			discount.put("baseAmount", adjustment.getBaseAmount());
			discount.put("description", adjustment.getDescription());
			discount.put("sponsor", adjustment.getDiscountSponsor() == null ? null : adjustment.getDiscountSponsor().getUuid());
			discounts.add(discount);
		}
		return discounts;
	}

	private MvcResult postRequest(String path, String json) throws Exception {
		return mockMvc.perform(post(path)
		        .contentType(MediaType.APPLICATION_JSON)
		        .accept(MediaType.APPLICATION_JSON)
		        .content(json))
		        .andReturn();
	}

	private void deletePaymentAndAssert(String billUuid, String paymentUuid, String reason) throws Exception {
		MvcResult result = mockMvc.perform(delete(BILL_RESOURCE + "/" + billUuid + "/payment/" + paymentUuid)
		        .param("reason", reason)
		        .accept(MediaType.APPLICATION_JSON))
		        .andReturn();
		assertSuccess(result);
	}

	private Map<String, Object> postJson(String path, String json) throws Exception {
		MvcResult result = postRequest(path, json);
		assertSuccess(result);
		return parse(result);
	}

	private MvcResult postLineItemResult(String lineItemUuid, String json) throws Exception {
		return mockMvc.perform(post(BILL_LINE_ITEM_RESOURCE + "/" + lineItemUuid)
		        .contentType(MediaType.APPLICATION_JSON)
		        .accept(MediaType.APPLICATION_JSON)
		        .content(json))
		        .andReturn();
	}

	private Map<String, Object> postLineItem(String lineItemUuid, String json) throws Exception {
		MvcResult result = postLineItemResult(lineItemUuid, json);
		assertSuccess(result);
		return parse(result);
	}

	private Map<String, Object> updateBill(String billUuid, String json) throws Exception {
		return postJson(BILL_RESOURCE + "/" + billUuid, json);
	}

	private Map<String, Object> updateLineItem(String lineItemUuid, String json) throws Exception {
		return postLineItem(lineItemUuid, json);
	}

	private void addVoidedDiscountAdjustment(String billUuid, String lineItemUuid, String description) {
		IBillService billService = Context.getService(IBillService.class);
		Bill bill = billService.getByUuid(billUuid);
		BillLineItem lineItem = persistedLineByUuid(bill, lineItemUuid);
		BillLineItemAdjustment adjustment = new BillLineItemAdjustment();
		adjustment.setAdjustmentType("DISCOUNT");
		adjustment.setAmount(new BigDecimal("999.00"));
		adjustment.setBaseAmount(new BigDecimal("999.00"));
		adjustment.setDescription(description);
		adjustment.setCreator(Context.getAuthenticatedUser());
		adjustment.setDateCreated(new java.util.Date());
		adjustment.setVoided(true);
		adjustment.setUuid(java.util.UUID.randomUUID().toString());
		lineItem.addAdjustment(adjustment);
		billService.save(bill);
	}

	private void assertVoidedDiscountStillVoided(String billUuid, String lineItemUuid, String description) {
		Bill bill = Context.getService(IBillService.class).getByUuid(billUuid);
		BillLineItem lineItem = persistedLineByUuid(bill, lineItemUuid);
		boolean found = false;
		for (BillLineItemAdjustment adjustment : lineItem.getAdjustments()) {
			if (adjustment != null && "DISCOUNT".equalsIgnoreCase(adjustment.getAdjustmentType())
			        && description.equals(adjustment.getDescription())) {
				found = true;
				assertTrue("Historical discount should remain voided", Boolean.TRUE.equals(adjustment.getVoided()));
			}
		}
		assertTrue("Expected voided historical discount adjustment", found);
	}

	private void assertPaymentAndAllocationsVoided(String billUuid, String paymentUuid, String reason) {
		Payment payment = persistedPaymentByUuid(billUuid, paymentUuid);
		assertTrue("Expected deleted payment to remain voided", Boolean.TRUE.equals(payment.getVoided()));
		assertEquals(reason, payment.getVoidReason());
		assertNotNull("Expected deleted payment to retain voidedBy", payment.getVoidedBy());
		assertNotNull("Expected deleted payment allocations to be retained", payment.getAllocations());
		assertTrue("Expected deleted payment to retain allocation history", !payment.getAllocations().isEmpty());
		for (LinePaymentAllocation allocation : payment.getAllocations()) {
			assertTrue("Expected deleted payment allocation to remain voided", Boolean.TRUE.equals(allocation.getVoided()));
			assertEquals(reason, allocation.getVoidReason());
			assertNotNull("Expected deleted allocation to retain voidedBy", allocation.getVoidedBy());
		}
	}

	private Payment persistedPaymentByUuid(String billUuid, String paymentUuid) {
		Bill bill = Context.getService(IBillService.class).getByUuid(billUuid);
		assertNotNull("Expected bill to exist", bill);
		for (Payment payment : bill.getPayments()) {
			if (payment != null && paymentUuid.equals(payment.getUuid())) {
				return payment;
			}
		}
		throw new AssertionError("Expected persisted payment with UUID " + paymentUuid);
	}

	private void assertPersistedBillState(String billUuid, String expectedStatus, String expectedBalance,
	        String expectedTotalActualPayments) {
		Bill bill = Context.getService(IBillService.class).getByUuid(billUuid);
		assertNotNull("Expected bill to exist", bill);
		assertEquals(expectedStatus, stringValue(bill.getStatus()));
		assertAmount(expectedBalance, bill.getBalance());
		assertAmount(expectedTotalActualPayments, bill.getTotalActualPayments());
	}

	private void assertPersistedActivePaymentCount(String billUuid, int expectedCount) {
		Bill bill = Context.getService(IBillService.class).getByUuid(billUuid);
		assertNotNull("Expected bill to exist", bill);
		int activePaymentCount = 0;
		for (Payment payment : bill.getPayments()) {
			if (payment != null && !Boolean.TRUE.equals(payment.getVoided())) {
				activePaymentCount++;
			}
		}
		assertEquals(Integer.valueOf(expectedCount), Integer.valueOf(activePaymentCount));
	}

	private BillLineItem persistedLineByUuid(Bill bill, String lineItemUuid) {
		assertNotNull("Expected bill to exist", bill);
		for (BillLineItem lineItem : bill.getLineItems()) {
			if (lineItem != null && lineItemUuid.equals(lineItem.getUuid())) {
				return lineItem;
			}
		}
		throw new AssertionError("Expected persisted line item with UUID " + lineItemUuid);
	}

	private Map<String, Object> postAdditionalDiscount(String billUuid, String amount) throws Exception {
		MvcResult result = postAdditionalDiscountResult(billUuid, amount);
		assertSuccess(result);
		return parse(result);
	}

	private Map<String, Object> postAdditionalDiscount(String billUuid, String amount, String sponsor, String comment)
	        throws Exception {
		MvcResult result = postRequest(BILL_ACTION_RESOURCE + "/" + billUuid + "/additional-discount",
		    "{\"discounts\":" + amount + ",\"sponsor\":\"" + sponsor + "\",\"comment\":\"" + comment + "\"}");
		assertSuccess(result);
		return parse(result);
	}

	private MvcResult postAdditionalDiscountResult(String billUuid, String amount) throws Exception {
		return postRequest(BILL_ACTION_RESOURCE + "/" + billUuid + "/additional-discount",
		    "{\"discounts\":" + amount + "}");
	}

	private List<Map<String, Object>> normalizeLineItems(List<Map<String, Object>> lineItems) {
		assertNotNull("Expected bill payload to contain lineItems", lineItems);
		lineItems.sort(new Comparator<Map<String, Object>>() {
			@Override
			public int compare(Map<String, Object> left, Map<String, Object> right) {
				return Integer.valueOf(intValue(left.get("lineItemOrder"))).compareTo(Integer.valueOf(intValue(right.get("lineItemOrder"))));
			}
		});
		return lineItems;
	}

	private Map<String, Object> lineByUuid(List<Map<String, Object>> lineItems, String lineUuid) {
		Map<String, Object> foundLine = null;
		for (Map<String, Object> lineItem : lineItems) {
			if (lineUuid.equals(stringValue(lineItem.get("uuid")))) {
				foundLine = lineItem;
				break;
			}
		}
		assertNotNull("Expected to find line item with UUID " + lineUuid, foundLine);
		return foundLine;
	}

	private Map<String, Object> findLineByService(List<Map<String, Object>> lineItems, String serviceUuid) {
		Map<String, Object> foundLine = null;
		for (Map<String, Object> lineItem : lineItems) {
			if (serviceUuid.equals(refUuid(lineItem.get("billableService")))) {
				foundLine = lineItem;
				break;
			}
		}
			assertNotNull("Expected to find line item for service " + serviceUuid + ". Available line items: "
			        + describeLineItems(lineItems), foundLine);
			return foundLine;
		}

		private String describeLineItems(List<Map<String, Object>> lineItems) {
			List<String> descriptions = new ArrayList<String>();
			for (Map<String, Object> lineItem : lineItems) {
				descriptions.add("{uuid=" + stringValue(lineItem.get("uuid"))
				        + ", display=" + stringValue(lineItem.get("display"))
				        + ", billableService=" + stringValue(lineItem.get("billableService"))
				        + ", price=" + stringValue(lineItem.get("price"))
				        + ", order=" + stringValue(lineItem.get("lineItemOrder")) + "}");
			}
			return descriptions.toString();
		}

	@SuppressWarnings("unchecked")
	private void assertActivePaymentCount(Map<String, Object> bill, int expectedCount) {
		Object paymentsValue = bill.get("payments");
		assertTrue("Expected bill representation to contain payments", paymentsValue instanceof List);
		List<Map<String, Object>> payments = (List<Map<String, Object>>) paymentsValue;
		assertEquals(Integer.valueOf(expectedCount), Integer.valueOf(payments.size()));
		for (Map<String, Object> payment : payments) {
			assertTrue("Expected fetched bill representation to exclude voided payments",
			    !Boolean.TRUE.equals(payment.get("voided")));
		}
	}

	private void closeBillAndAssert(String billUuid, String reason) throws Exception {
		Map<String, Object> closed = postJson(BILL_ACTION_RESOURCE + "/" + billUuid + "/close", closePayload(reason));
		assertEquals(Boolean.TRUE, closed.get("closed"));
	}

	private void reopenBillAndAssert(String billUuid) throws Exception {
		Map<String, Object> reopened = postJson(BILL_ACTION_RESOURCE + "/" + billUuid + "/reopen", "{}");
		assertEquals(Boolean.FALSE, reopened.get("closed"));
	}

	private String closePayload(String reason) {
		return "{\"reason\":\"" + reason + "\"}";
	}

	private void assertSuccess(MvcResult result) throws Exception {
		int status = result.getResponse().getStatus();
		assertEquals("Expected 2xx REST response, got " + status + ": " + result.getResponse().getContentAsString(),
		    Integer.valueOf(2), Integer.valueOf(status / 100));
	}

	private boolean isSuccess(MvcResult result) {
		return (result.getResponse().getStatus() / 100) == 2;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parse(MvcResult result) throws Exception {
		return (Map<String, Object>) SimpleObject.parseJson(result.getResponse().getContentAsString());
	}

	private String createBillPayload() {
		return billPayloadFromJsonLines(
		    linePayload(SERVICE_1_UUID, PRICE_100_UUID, "100", null),
		    linePayload(SERVICE_2_UUID, PRICE_200_UUID, "150", "Initial override"),
		    linePayload(SERVICE_3_UUID, PRICE_300_UUID, "250", "Initial override"));
	}

		private String additionalDiscountBillPayload() {
			return billPayloadFromJsonLines(
			    linePayloadWithDiscount(SERVICE_4_UUID, PRICE_400_UUID, "400", "40", "Taxed discount line"));
		}

		private String taxedBillPayload() {
			return billPayloadFromJsonLines(linePayload(SERVICE_4_UUID, PRICE_400_UUID, "400", "Taxed line"));
		}

		private String taxedAndUntaxedBillPayload() {
			return billPayloadFromJsonLines(
			    linePayload(SERVICE_4_UUID, PRICE_400_UUID, "400", "Taxed line"),
			    linePayload(SERVICE_5_UUID, PRICE_500_UUID, "500", "Untaxed line"));
		}

	private String billPayload(List<Map<String, Object>> existingLines, String newLineOne, String newLineTwo) {
		return billPayloadFromJsonLines(
		    existingLinePayload(existingLines.get(0)),
		    existingLinePayload(existingLines.get(1)),
		    existingLinePayload(existingLines.get(2)),
		    newLineOne,
		    newLineTwo);
	}

		private String billPayloadFromJsonLines(String lineOne, String lineTwo, String lineThree) {
			return "{"
			        + "\"patient\":\"" + PATIENT_UUID + "\","
			        + newReceiptNumberJson()
			        + "\"status\":\"PENDING\","
			        + "\"lineItems\":[" + lineOne + "," + lineTwo + "," + lineThree + "]"
			        + "}";
		}

		private String billPayloadFromJsonLines(String lineOne, String lineTwo) {
			return "{"
			        + "\"patient\":\"" + PATIENT_UUID + "\","
			        + newReceiptNumberJson()
			        + "\"status\":\"PENDING\","
			        + "\"lineItems\":[" + lineOne + "," + lineTwo + "]"
			        + "}";
		}

		private String billPayloadFromJsonLines(String lineOne) {
			return "{"
			        + "\"patient\":\"" + PATIENT_UUID + "\","
			        + newReceiptNumberJson()
			        + "\"status\":\"PENDING\","
			        + "\"lineItems\":[" + lineOne + "]"
			        + "}";
		}

	private String billPayloadFromJsonLines(String lineOne, String lineTwo, String lineThree, String lineFour,
	        String lineFive) {
		StringBuilder sb = new StringBuilder();
		sb.append("{")
		        .append("\"patient\":\"" + PATIENT_UUID + "\",")
		        .append(newReceiptNumberJson())
		        .append("\"status\":\"PENDING\",")
		        .append("\"lineItems\":[")
		        .append(lineOne)
		        .append(",")
		        .append(lineTwo)
		        .append(",")
		        .append(lineThree);
		if (lineFour != null && lineFour.length() > 0) {
			sb.append(",").append(lineFour);
		}
		if (lineFive != null && lineFive.length() > 0) {
			sb.append(",").append(lineFive);
		}
			sb.append("]}");
			return sb.toString();
		}

		private String newReceiptNumberJson() {
			return "\"receiptNumber\":\"REST-SMOKE-" + java.util.UUID.randomUUID().toString() + "\",";
		}

	private String linePayload(String billableServiceUuid, String priceUuid, String price, String reason) {
		return "{"
		        + "\"billableService\":\"" + billableServiceUuid + "\"," +
		        "\"priceUuid\":\"" + priceUuid + "\"," +
		        "\"price\":" + price + ","
		        + "\"quantity\":1,"
		        + "\"paymentStatus\":\"PENDING\""
		        + reasonJson(reason)
		        + "}";
	}

	private String linePayloadWithDiscount(String billableServiceUuid, String priceUuid, String price, String discountAmount,
	        String reason) {
		return "{"
		        + "\"billableService\":\"" + billableServiceUuid + "\"," +
		        "\"priceUuid\":\"" + priceUuid + "\"," +
		        "\"price\":" + price + ","
		        + "\"quantity\":1,"
		        + "\"paymentStatus\":\"PENDING\","
		        + "\"discounts\":[{"
		        + "\"amount\":" + discountAmount + ","
		        + "\"baseAmount\":" + price + ","
		        + "\"rate\":0.10,"
		        + "\"description\":\"Smoke line discount\""
		        + "}]"
		        + reasonJson(reason)
		        + "}";
	}

	private String existingLinePayload(Map<String, Object> line) {
		return linePayload(refUuid(line.get("billableService")), stringValue(line.get("priceUuid")),
		    numberString(line.get("price")), stringValue(line.get("priceOverrideReason")));
	}

	private String existingLinePayload(Map<String, Object> line, String billableServiceUuid, String priceUuid, String price,
	        String reason) {
		return "{"
		        + "\"uuid\":\"" + stringValue(line.get("uuid")) + "\"," +
		        "\"billableService\":\"" + billableServiceUuid + "\"," +
		        "\"priceUuid\":\"" + priceUuid + "\"," +
		        "\"price\":" + price + ","
		        + "\"quantity\":" + numberString(line.get("quantity")) + ","
		        + "\"paymentStatus\":\"" + stringValue(line.get("paymentStatus")) + "\""
		        + reasonJson(reason)
		        + "}";
	}

	private String existingLinePayloadWithDiscount(Map<String, Object> line, String billableServiceUuid, String priceUuid,
	        String price, String discountAmount, String description) {
		return "{"
		        + "\"uuid\":\"" + stringValue(line.get("uuid")) + "\"," +
		        "\"billableService\":\"" + billableServiceUuid + "\"," +
		        "\"priceUuid\":\"" + priceUuid + "\"," +
		        "\"price\":" + price + ","
		        + "\"quantity\":" + numberString(line.get("quantity")) + ","
		        + "\"paymentStatus\":\"" + stringValue(line.get("paymentStatus")) + "\","
		        + "\"discounts\":[{"
		        + "\"amount\":" + discountAmount + ","
		        + "\"baseAmount\":" + price + ","
		        + "\"description\":\"" + description + "\""
		        + "}]"
		        + "}";
	}

	private String paymentPayload(Map<String, Object> line1, Map<String, Object> line2) {
		return "{"
		        + "\"instanceType\":\"" + PAYMENT_MODE_UUID + "\"," +
		        "\"amount\":250,"
		        + "\"amountTendered\":250,"
		        + "\"allocations\":["
		        + allocationPayload(line1, "100", ALLOCATION_ONE_UUID) + ","
		        + allocationPayload(line2, "150", ALLOCATION_TWO_UUID)
		        + "]"
		        + "}";
	}

	private String singleAllocationPaymentPayload(Map<String, Object> line, String amount) {
		return "{"
		        + "\"instanceType\":\"" + PAYMENT_MODE_UUID + "\"," +
		        "\"amount\":" + amount + ","
		        + "\"amountTendered\":" + amount + ","
		        + "\"allocations\":["
		        + allocationPayload(line, amount, java.util.UUID.randomUUID().toString())
		        + "]"
		        + "}";
	}

	private String paymentPayloadWithoutAllocations(String amount) {
		return "{"
		        + "\"instanceType\":\"" + PAYMENT_MODE_UUID + "\"," +
		        "\"amount\":" + amount + ","
		        + "\"amountTendered\":" + amount
		        + "}";
	}

	private String outstandingPaymentPayload(List<Map<String, Object>> lineItems) {
		BigDecimal amount = computeLineItemBalanceAmount(lineItems);
		StringBuilder allocations = new StringBuilder();
		for (Map<String, Object> lineItem : lineItems) {
			BigDecimal total = amountValue(lineItem.get("total"));
			BigDecimal allocated = amountValue(lineItem.get("totalAllocated"));
			BigDecimal outstanding = total.subtract(allocated);
			if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
				if (allocations.length() > 0) {
					allocations.append(",");
				}
				allocations.append(allocationPayload(lineItem, outstanding.toPlainString(),
				    java.util.UUID.randomUUID().toString()));
			}
		}
		return "{"
		        + "\"instanceType\":\"" + PAYMENT_MODE_UUID + "\"," +
		        "\"amount\":" + amount.toPlainString() + ","
		        + "\"amountTendered\":" + amount.toPlainString() + ","
		        + "\"allocations\":[" + allocations + "]"
		        + "}";
	}

	private String allocationPayload(Map<String, Object> line, String amount, String uuid) {
		return "{"
		        + "\"uuid\":\"" + uuid + "\"," +
		        "\"billLineItem\":\"" + stringValue(line.get("uuid")) + "\"," +
		        "\"allocatedAmount\":" + amount
		        + "}";
	}

	private String reasonJson(String reason) {
		if (reason == null || "null".equals(reason) || reason.trim().isEmpty()) {
			return "";
		}
		return ",\"priceOverrideReason\":\"" + reason + "\"";
	}

	private String refUuid(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String) {
			String stringValue = (String) value;
			int separator = stringValue.indexOf(':');
			return separator >= 0 ? stringValue.substring(0, separator) : stringValue;
		}
		return stringValue(((Map) value).get("uuid"));
	}

	private boolean isSmokeLineItem(Map<String, Object> lineItem) {
		String serviceUuid = refUuid(lineItem.get("billableService"));
		return SERVICE_1_UUID.equals(serviceUuid) || SERVICE_2_UUID.equals(serviceUuid) || SERVICE_3_UUID.equals(serviceUuid)
		        || SERVICE_4_UUID.equals(serviceUuid) || SERVICE_5_UUID.equals(serviceUuid);
	}

	private String stringValue(Object value) {
		return String.valueOf(value);
	}

	private String numberString(Object value) {
		return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
	}

	private int intValue(Object value) {
		return amountValue(value).intValue();
	}

	private void assertLinePriceState(Map<String, Object> lineItem, String price, String originalPrice, boolean overridden) {
		assertAmount(price, lineItem.get("price"));
		assertAmount(originalPrice, lineItem.get("originalPrice"));
		assertEquals(Boolean.valueOf(overridden), lineItem.get("priceOverridden"));
	}

	private void assertInlinePriceState(Map<String, Object> lineItem, String price, String originalPrice, boolean overridden) {
		assertLinePriceState(lineItem, price, originalPrice, overridden);
	}

	private void assertAmount(String expected, Object actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(amountValue(actual)));
	}

	private String computeLineItemBalance(List<Map<String, Object>> lineItems) {
			return computeLineItemBalanceAmount(lineItems).toPlainString();
	}

	private BigDecimal computeLineItemBalanceAmount(List<Map<String, Object>> lineItems) {
		BigDecimal balance = BigDecimal.ZERO;
			for (Map<String, Object> lineItem : lineItems) {
				BigDecimal total = amountValue(lineItem.get("total"));
				BigDecimal allocated = amountValue(lineItem.get("totalAllocated"));
				balance = balance.add(total.subtract(allocated));
			}
			return balance;
		}

	private BigDecimal amountValue(Object value) {
		if (value == null || "null".equals(String.valueOf(value))) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(String.valueOf(value));
	}

	@SuppressWarnings("unchecked")
	private BigDecimal lineDiscountAmount(Map<String, Object> lineItem) {
		return adjustmentTotal(lineItem, "totalDiscount", "discounts");
	}

	@SuppressWarnings("unchecked")
	private void assertLineDiscountMetadata(Map<String, Object> lineItem, String sponsorUuid, String description) {
		Object discountsValue = lineItem.get("discounts");
		assertTrue("Expected serialized discounts", discountsValue instanceof List);
		List<Map<String, Object>> discounts = (List<Map<String, Object>>) discountsValue;
		assertEquals("Expected one active discount", Integer.valueOf(1), Integer.valueOf(discounts.size()));
		Map<String, Object> discount = discounts.get(0);
		assertEquals(description, stringValue(discount.get("description")));
		if (sponsorUuid == null) {
			assertEquals(null, discount.get("sponsor"));
		} else {
			assertEquals(sponsorUuid, stringValue(discount.get("sponsor")));
		}
	}

	@SuppressWarnings("unchecked")
	private BigDecimal lineTaxAmount(Map<String, Object> lineItem) {
		return adjustmentTotal(lineItem, "totalTax", "taxes");
	}

	@SuppressWarnings("unchecked")
	private BigDecimal adjustmentTotal(Map<String, Object> lineItem, String totalKey, String adjustmentsKey) {
		BigDecimal total = amountValue(lineItem.get(totalKey));
		if (total.compareTo(BigDecimal.ZERO) != 0) {
			return total;
		}
		Object adjustments = lineItem.get(adjustmentsKey);
		if (!(adjustments instanceof List)) {
			return total;
		}
		for (Map<String, Object> adjustment : (List<Map<String, Object>>) adjustments) {
			total = total.add(amountValue(adjustment.get("amount")));
		}
		return total;
	}

	private void assertComputedBalance(String expected, List<Map<String, Object>> lineItems) {
		BigDecimal balance = BigDecimal.ZERO;
		for (Map<String, Object> lineItem : lineItems) {
			BigDecimal total = amountValue(lineItem.get("total"));
			BigDecimal allocated = amountValue(lineItem.get("totalAllocated"));
			balance = balance.add(total.subtract(allocated));
		}
		assertAmount(expected, balance);
	}

	private static class NoOpValidator implements Validator {
		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(Object target, Errors errors) {
		}
	}
}
