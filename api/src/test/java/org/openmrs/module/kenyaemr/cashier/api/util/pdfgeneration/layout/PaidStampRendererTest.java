package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import org.junit.Test;
import org.openmrs.module.kenyaemr.cashier.api.model.Bill;
import org.openmrs.module.kenyaemr.cashier.api.model.BillLineItem;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaidStampRendererTest {

    @Test
    public void isPaidStampEnabled_shouldDefaultToEnabled() {
        assertTrue(PaidStampRenderer.isPaidStampEnabled(null));
        assertTrue(PaidStampRenderer.isPaidStampEnabled(""));
        assertTrue(PaidStampRenderer.isPaidStampEnabled("true"));
        assertTrue(PaidStampRenderer.isPaidStampEnabled(" TRUE "));
    }

    @Test
    public void isPaidStampEnabled_shouldDisableOnlyWhenExplicitlyFalse() {
        assertFalse(PaidStampRenderer.isPaidStampEnabled("false"));
        assertFalse(PaidStampRenderer.isPaidStampEnabled(" FALSE "));
    }

    @Test
    public void shouldRender_shouldRespectPaidStampConfiguration() {
        Bill bill = paidBill();

        assertTrue(PaidStampRenderer.shouldRender(bill, true));
        assertFalse(PaidStampRenderer.shouldRender(bill, false));
    }

    private Bill paidBill() {
        Bill bill = new Bill();
        bill.setStatus(BillStatus.PAID);
        bill.setLineItems(new ArrayList<BillLineItem>(Arrays.asList(lineItemWithPrice("100.00"))));
        bill.setPayments(new HashSet<Payment>(Arrays.asList(paymentWithAmount("100.00"))));
        return bill;
    }

    private BillLineItem lineItemWithPrice(String amount) {
        BillLineItem item = new BillLineItem();
        item.setPrice(new BigDecimal(amount));
        item.setQuantity(1);
        item.setVoided(false);
        return item;
    }

    private Payment paymentWithAmount(String amount) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal(amount));
        payment.setAmountTendered(new BigDecimal(amount));
        payment.setVoided(false);
        return payment;
    }
}
