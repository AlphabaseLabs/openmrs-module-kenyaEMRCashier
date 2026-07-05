package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.junit.Before;
import org.junit.Test;
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
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
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
		List<Map<String, Object>> lineItems = getSmokeLineItems();
		assertEquals(3, lineItems.size());
		assertInlinePriceState(findLineByService(lineItems, SERVICE_1_UUID), "100.00", "100.00", false);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_2_UUID), "150.00", "200.00", true);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "250.00", "300.00", true);

		// C. Add normal + overridden lines at bill level only
		updateBill(billUuid,
		    billPayload(lineItems,
		        linePayload(SERVICE_4_UUID, PRICE_400_UUID, "400", null),
		        linePayload(SERVICE_5_UUID, PRICE_500_UUID, "450", "Added override")));
		lineItems = getSmokeLineItems();
		assertEquals(5, lineItems.size());
		assertInlinePriceState(findLineByService(lineItems, SERVICE_1_UUID), "100.00", "100.00", false);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_2_UUID), "150.00", "200.00", true);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "250.00", "300.00", true);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_4_UUID), "400.00", "400.00", false);
		assertInlinePriceState(findLineByService(lineItems, SERVICE_5_UUID), "450.00", "500.00", true);

		// D. Reset a line by priceUuid through /billLineItem/{uuid}
		Map<String, Object> thirdLine = findLineByService(lineItems, SERVICE_3_UUID);
		updateLineItem(stringValue(thirdLine.get("uuid")),
		    existingLinePayload(thirdLine, SERVICE_3_UUID, PRICE_425_UUID, "425", null));
		lineItems = getSmokeLineItems();
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "425.00", "425.00", false);

		// E. Manual price override after reset
		thirdLine = findLineByService(lineItems, SERVICE_3_UUID);
		updateLineItem(stringValue(thirdLine.get("uuid")),
		    existingLinePayload(thirdLine, SERVICE_3_UUID, PRICE_425_UUID, "350", "Manual override after reset"));
		lineItems = getSmokeLineItems();
		assertInlinePriceState(findLineByService(lineItems, SERVICE_3_UUID), "350.00", "425.00", true);

		// F. Payment that covers first two lines
		Map<String, Object> firstLine = findLineByService(lineItems, SERVICE_1_UUID);
		Map<String, Object> secondLine = findLineByService(lineItems, SERVICE_2_UUID);
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", paymentPayload(firstLine, secondLine));
		lineItems = getSmokeLineItems();
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
			assertEquals("PAID", stringValue(paidLineAfterEdit.get("paymentStatus")));
			lineItems = getSmokeLineItems();
			assertComputedBalance(computeLineItemBalance(lineItems), lineItems);
		} else {
			int status = openPaidLineUpdate.getResponse().getStatus();
			assertTrue("Paid line edit policy should be explicit at API boundary", status >= 400 && status < 500);
			lineItems = getSmokeLineItems();
			assertInlinePriceState(lineByUuid(lineItems, stringValue(secondLine.get("uuid"))), "150.00", "200.00", true);
		}

		// H. Close-edit-reopen-edit-close behavior
		lineItems = getSmokeLineItems();
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", outstandingPaymentPayload(lineItems));
		lineItems = getSmokeLineItems();
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
		lineItems = getSmokeLineItems();
		postJson(BILL_RESOURCE + "/" + billUuid + "/payment", outstandingPaymentPayload(lineItems));
		closeBillAndAssert(billUuid, "Close bill again after reopen");
	}

	private Map<String, Object> getLineItem(String uuid) throws Exception {
		MvcResult result = mockMvc.perform(get(BILL_LINE_ITEM_RESOURCE + "/" + uuid)
		        .accept(MediaType.APPLICATION_JSON))
		        .andReturn();
		assertSuccess(result);
		return parse(result);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getSmokeLineItems() throws Exception {
		MvcResult result = mockMvc.perform(get(BILL_LINE_ITEM_RESOURCE)
		        .param("v", "full")
		        .param("limit", "100")
		        .accept(MediaType.APPLICATION_JSON))
		        .andReturn();
		assertSuccess(result);
		Map<String, Object> payload = parse(result);
		List<Map<String, Object>> results = (List<Map<String, Object>>) payload.get("results");
		if (results == null) {
			fail("Expected billLineItem collection payload to contain results. Payload keys=" + payload.keySet());
		}

		List<Map<String, Object>> lineItems = new ArrayList<>();
		for (Map<String, Object> lineItem : results) {
			if (isSmokeLineItem(lineItem) && !LEGACY_LINE_ITEM_UUID.equals(stringValue(lineItem.get("uuid")))) {
				lineItems.add(lineItem);
			}
		}
		return normalizeLineItems(lineItems);
	}

	private MvcResult postRequest(String path, String json) throws Exception {
		return mockMvc.perform(post(path)
		        .contentType(MediaType.APPLICATION_JSON)
		        .accept(MediaType.APPLICATION_JSON)
		        .content(json))
		        .andReturn();
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
		assertNotNull("Expected to find line item for service " + serviceUuid, foundLine);
		return foundLine;
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

	private String billPayload(List<Map<String, Object>> existingLines, String newLineOne, String newLineTwo) {
		return billPayloadFromJsonLines(
		    existingLinePayload(existingLines.get(0)),
		    existingLinePayload(existingLines.get(1)),
		    existingLinePayload(existingLines.get(2)),
		    newLineOne,
		    newLineTwo);
	}

	private String billPayloadFromJsonLines(String lineOne, String lineTwo, String lineThree) {
		return billPayloadFromJsonLines(lineOne, lineTwo, lineThree, "", "");
	}

	private String billPayloadFromJsonLines(String lineOne, String lineTwo, String lineThree, String lineFour,
	        String lineFive) {
		StringBuilder sb = new StringBuilder();
		sb.append("{")
		        .append("\"patient\":\"" + PATIENT_UUID + "\",")
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

	private String outstandingPaymentPayload(List<Map<String, Object>> lineItems) {
		BigDecimal amount = computeLineItemBalanceAmount(lineItems);
		StringBuilder allocations = new StringBuilder();
		for (Map<String, Object> lineItem : lineItems) {
			BigDecimal total = new BigDecimal(String.valueOf(lineItem.get("total")));
			BigDecimal allocated = new BigDecimal(String.valueOf(lineItem.get("totalAllocated")));
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
		return new BigDecimal(String.valueOf(value)).intValue();
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
		assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(actual))));
	}

	private String computeLineItemBalance(List<Map<String, Object>> lineItems) {
			return computeLineItemBalanceAmount(lineItems).toPlainString();
	}

	private BigDecimal computeLineItemBalanceAmount(List<Map<String, Object>> lineItems) {
		BigDecimal balance = BigDecimal.ZERO;
		for (Map<String, Object> lineItem : lineItems) {
			BigDecimal total = new BigDecimal(String.valueOf(lineItem.get("total")));
			BigDecimal allocated = new BigDecimal(String.valueOf(lineItem.get("totalAllocated")));
			balance = balance.add(total.subtract(allocated));
		}
		return balance;
	}

	private void assertComputedBalance(String expected, List<Map<String, Object>> lineItems) {
		BigDecimal balance = BigDecimal.ZERO;
		for (Map<String, Object> lineItem : lineItems) {
			BigDecimal total = new BigDecimal(String.valueOf(lineItem.get("total")));
			BigDecimal allocated = new BigDecimal(String.valueOf(lineItem.get("totalAllocated")));
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
