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
 * Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;

import java.math.BigDecimal;
import java.util.Date;

final class RestResourceConversionUtil {
	static final String DATE_CREATED_PROPERTY = "dateCreated";

	private RestResourceConversionUtil() {
	}

	static boolean containsDateCreated(SimpleObject properties) {
		return properties != null && properties.containsKey(DATE_CREATED_PROPERTY);
	}

	static Object removeDateCreated(SimpleObject properties) {
		return properties.remove(DATE_CREATED_PROPERTY);
	}

	static Date toDate(Object dateValue) {
		if (dateValue == null) {
			return null;
		}
		if (dateValue instanceof Date) {
			return (Date) dateValue;
		}
		if (dateValue instanceof Number) {
			return new Date(((Number) dateValue).longValue());
		}
		return (Date) ConversionUtil.convert(dateValue, Date.class);
	}

	static BigDecimal toBigDecimal(Object value, String fieldName) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}

		String rawValue = value.toString().trim();
		if (rawValue.isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(rawValue);
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException(fieldName + " must be numeric.", ex);
		}
	}
}
