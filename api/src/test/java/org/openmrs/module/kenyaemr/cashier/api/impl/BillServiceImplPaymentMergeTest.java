package org.openmrs.module.kenyaemr.cashier.api.impl;

import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMode;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentModeAttributeType;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BillServiceImplPaymentMergeTest {

	private static final Integer TRANSACTION_ID_TYPE_ID = 7001;
	private static final String TRANSACTION_ID_TYPE_UUID = "2bd26329-c429-4013-8ff3-090afa2e507d";

	static {
		File appDataDir = new File("target/openmrs-test-appdata");
		if (!appDataDir.exists()) {
			appDataDir.mkdirs();
		}
		System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDir.getAbsolutePath());
	}

	private final BillServiceImpl billService = new BillServiceImpl();

	@Test
	public void shouldMergeIncomingPayment_shouldSkipReplayWithSameUniqueAttributeOnPersistedPayment() {
		Bill existingBill = createBillWithPayments(createPayment(101, "existing-payment-uuid", false, "324552"));
		Payment incomingPayment = createPayment(null, null, false, "324552");

		assertFalse(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldAllowPaymentWhenUniqueAttributeValueDiffers() {
		Bill existingBill = createBillWithPayments(createPayment(101, "existing-payment-uuid", false, "324552"));
		Payment incomingPayment = createPayment(null, null, false, "998877");

		assertTrue(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldNotDeduplicateAgainstUnpersistedInMemoryPayment() {
		Bill existingBill = createBillWithPayments(createPayment(null, null, false, "324552"));
		Payment incomingPayment = createPayment(null, null, false, "324552");

		assertTrue(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldAllowSameUniqueAttributeWhenExistingPaymentIsVoided() {
		Bill existingBill = createBillWithPayments(createPayment(101, "existing-payment-uuid", true, "324552"));
		Payment incomingPayment = createPayment(null, null, false, "324552");

		assertTrue(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldSkipReplayWithSameUuid() {
		Bill existingBill = createBillWithPayments(createPayment(101, "existing-payment-uuid", false, null));
		Payment incomingPayment = createPayment(null, "existing-payment-uuid", false, null);

		assertFalse(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldSkipReplayForCashPaymentWithMatchingSignature() {
		Payment existingPayment = createPayment(101, "existing-payment-uuid", false, null);
		existingPayment.setInstanceType(createPaymentMode(22, "cash-mode-uuid", "Cash"));
		existingPayment.setAmount(BigDecimal.valueOf(1999));
		existingPayment.setAmountTendered(BigDecimal.valueOf(1999));
		existingPayment.setDateCreated(new Date(1771343896000L));

		Payment incomingPayment = createPayment(null, null, false, null);
		incomingPayment.setInstanceType(createPaymentMode(22, "cash-mode-uuid", "Cash"));
		incomingPayment.setAmount(BigDecimal.valueOf(1999));
		incomingPayment.setAmountTendered(BigDecimal.valueOf(1999));
		incomingPayment.setDateCreated(new Date(1771343896000L));

		Bill existingBill = createBillWithPayments(existingPayment);
		assertFalse(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	@Test
	public void shouldMergeIncomingPayment_shouldAllowCashPaymentWhenReplaySignatureDiffers() {
		Payment existingPayment = createPayment(101, "existing-payment-uuid", false, null);
		existingPayment.setInstanceType(createPaymentMode(22, "cash-mode-uuid", "Cash"));
		existingPayment.setAmount(BigDecimal.valueOf(1999));
		existingPayment.setAmountTendered(BigDecimal.valueOf(1999));
		existingPayment.setDateCreated(new Date(1771343896000L));

		Payment incomingPayment = createPayment(null, null, false, null);
		incomingPayment.setInstanceType(createPaymentMode(22, "cash-mode-uuid", "Cash"));
		incomingPayment.setAmount(BigDecimal.valueOf(1999));
		incomingPayment.setAmountTendered(BigDecimal.valueOf(1999));
		incomingPayment.setDateCreated(new Date(1771344896000L));

		Bill existingBill = createBillWithPayments(existingPayment);
		assertTrue(billService.shouldMergeIncomingPayment(existingBill, incomingPayment));
	}

	private Bill createBillWithPayments(Payment... payments) {
		Bill bill = new Bill();
		Set<Payment> paymentSet = new HashSet<Payment>();
		for (Payment payment : payments) {
			if (payment != null) {
				payment.setBill(bill);
				paymentSet.add(payment);
			}
		}
		bill.setPayments(paymentSet);
		return bill;
	}

	private Payment createPayment(Integer id, String uuid, boolean voided, String transactionIdValue) {
		Payment payment = new Payment();
		payment.setId(id);
		payment.setUuid(uuid);
		payment.setVoided(voided);
		payment.setAmount(BigDecimal.ONE);
		payment.setAmountTendered(BigDecimal.ONE);

		if (transactionIdValue != null) {
			PaymentAttribute attribute = new PaymentAttribute();
			attribute.setAttributeType(createTransactionIdAttributeType());
			attribute.setValue(transactionIdValue);
			attribute.setOwner(payment);

			Set<PaymentAttribute> attributes = new HashSet<PaymentAttribute>();
			attributes.add(attribute);
			payment.setAttributes(attributes);
		}
		return payment;
	}

	private PaymentModeAttributeType createTransactionIdAttributeType() {
		PaymentModeAttributeType attributeType = new PaymentModeAttributeType();
		attributeType.setId(TRANSACTION_ID_TYPE_ID);
		attributeType.setUuid(TRANSACTION_ID_TYPE_UUID);
		attributeType.setName("Transaction Id");
		return attributeType;
	}

	private PaymentMode createPaymentMode(Integer id, String uuid, String name) {
		PaymentMode mode = new PaymentMode();
		mode.setId(id);
		mode.setUuid(uuid);
		mode.setName(name);
		return mode;
	}
}
