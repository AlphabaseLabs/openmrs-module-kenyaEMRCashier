/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.api.util;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.kenyaemr.cashier.api.model.Payment;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentAttribute;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for payment replay/idempotency detection during bill merges.
 */
public final class PaymentReplayUtil {

	private PaymentReplayUtil() {
	}

	public static boolean isPersistedActive(Payment payment) {
		return payment != null && payment.getId() != null && !Boolean.TRUE.equals(payment.getVoided());
	}

	public static String getAttributeTypeKey(PaymentAttribute attribute) {
		if (attribute == null || attribute.getAttributeType() == null) {
			return null;
		}
		if (attribute.getAttributeType().getId() != null) {
			return "id:" + attribute.getAttributeType().getId();
		}
		if (StringUtils.isNotBlank(attribute.getAttributeType().getUuid())) {
			return "uuid:" + attribute.getAttributeType().getUuid().trim();
		}
		if (StringUtils.isNotBlank(attribute.getAttributeType().getName())) {
			return "name:" + attribute.getAttributeType().getName().trim();
		}
		return null;
	}

	public static Set<String> getAttributeValueKeys(Payment payment) {
		Set<String> keys = new HashSet<String>();
		if (payment == null || payment.getAttributes() == null) {
			return keys;
		}

		for (PaymentAttribute attribute : payment.getAttributes()) {
			String attributeTypeKey = getAttributeTypeKey(attribute);
			if (attributeTypeKey == null || StringUtils.isBlank(attribute.getValue())) {
				continue;
			}
			keys.add(attributeTypeKey + ":" + attribute.getValue().trim());
		}

		return keys;
	}

	public static String getReplaySignature(Payment payment) {
		if (payment == null) {
			return null;
		}

		String modeKey = getModeKey(payment);
		String amount = normalizeAmount(payment.getAmount());
		String amountTendered = normalizeAmount(payment.getAmountTendered());
		Long dateCreated = payment.getDateCreated() == null ? null : payment.getDateCreated().getTime();
		String itemKey = getItemKey(payment);

		if (modeKey == null || amount == null || amountTendered == null || dateCreated == null) {
			return null;
		}

		return modeKey + "|" + amount + "|" + amountTendered + "|" + dateCreated + "|"
		        + (itemKey == null ? "" : itemKey);
	}

	/**
	 * Determines whether incomingPayment is a replay of existingPayment.
	 * Attribute/signature matching is limited to persisted active existing payments.
	 */
	public static boolean isReplayOf(Payment existingPayment, Payment incomingPayment) {
		if (existingPayment == null || incomingPayment == null) {
			return false;
		}

		if (incomingPayment.getId() != null && existingPayment.getId() != null
		        && incomingPayment.getId().equals(existingPayment.getId())) {
			return true;
		}

		String incomingUuid = StringUtils.trimToNull(incomingPayment.getUuid());
		String existingUuid = StringUtils.trimToNull(existingPayment.getUuid());
		if (incomingUuid != null && existingUuid != null && incomingUuid.equals(existingUuid)) {
			return true;
		}

		if (!isPersistedActive(existingPayment)) {
			return false;
		}

		Set<String> incomingAttributeKeys = getAttributeValueKeys(incomingPayment);
		if (!incomingAttributeKeys.isEmpty()) {
			Set<String> existingAttributeKeys = getAttributeValueKeys(existingPayment);
			for (String incomingKey : incomingAttributeKeys) {
				if (existingAttributeKeys.contains(incomingKey)) {
					return true;
				}
			}
		}

		String incomingReplaySignature = getReplaySignature(incomingPayment);
		String existingReplaySignature = getReplaySignature(existingPayment);
		return incomingReplaySignature != null && incomingReplaySignature.equals(existingReplaySignature);
	}

	private static String getModeKey(Payment payment) {
		if (payment == null || payment.getInstanceType() == null) {
			return null;
		}
		if (payment.getInstanceType().getId() != null) {
			return "id:" + payment.getInstanceType().getId();
		}
		if (StringUtils.isNotBlank(payment.getInstanceType().getUuid())) {
			return "uuid:" + payment.getInstanceType().getUuid().trim();
		}
		if (StringUtils.isNotBlank(payment.getInstanceType().getName())) {
			return "name:" + payment.getInstanceType().getName().trim();
		}
		return null;
	}

	private static String getItemKey(Payment payment) {
		if (payment == null || payment.getItem() == null) {
			return null;
		}
		if (payment.getItem().getId() != null) {
			return "id:" + payment.getItem().getId();
		}
		if (StringUtils.isNotBlank(payment.getItem().getUuid())) {
			return "uuid:" + payment.getItem().getUuid().trim();
		}
		return null;
	}

	private static String normalizeAmount(BigDecimal value) {
		if (value == null) {
			return null;
		}
		return value.stripTrailingZeros().toPlainString();
	}
}

