package org.openmrs.module.kenyaemr.cashier.rest.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.kenyaemr.cashier.api.model.BillStatus;
import org.openmrs.module.kenyaemr.cashier.api.model.HistorySearchCriteria;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.ConversionUtil;

public final class HistoryCriteriaBuilder {
	private static final int DEFAULT_LIMIT = 10;
	private static final int DEFAULT_START_INDEX = 0;

	private HistoryCriteriaBuilder() {
	}

	public static HistorySearchCriteria fromRequestContext(RequestContext context) {
		HistorySearchCriteria criteria = newCriteria(resolveLimit(context), resolveStartIndex(context));
		applyCommonParameters(criteria, context.getParameter("fromDate"), context.getParameter("toDate"),
		    context.getParameter("patientUuid"), context.getParameter("status"), context.getParameter("paymentModes"),
		    context.getParameter("cashierUuids"), context.getParameter("timesheetUuid"));
		return criteria;
	}

	public static HistorySearchCriteria fromRequestParams(String fromDate, String toDate, String patientUuid, String status,
	        String paymentModes, String cashierUuids, String timesheetUuid) {
		HistorySearchCriteria criteria = newCriteria(null, null);
		applyCommonParameters(criteria, fromDate, toDate, patientUuid, status, paymentModes, cashierUuids, timesheetUuid);
		return criteria;
	}

	private static HistorySearchCriteria newCriteria(Integer limit, Integer startIndex) {
		HistorySearchCriteria criteria = new HistorySearchCriteria();
		criteria.setLimit(limit);
		criteria.setStartIndex(startIndex);
		return criteria;
	}

	private static Integer resolveLimit(RequestContext context) {
		return context.getLimit() == null ? Integer.valueOf(DEFAULT_LIMIT) : context.getLimit();
	}

	private static Integer resolveStartIndex(RequestContext context) {
		return context.getStartIndex() == null ? Integer.valueOf(DEFAULT_START_INDEX) : context.getStartIndex();
	}

	private static void applyCommonParameters(HistorySearchCriteria criteria, String fromDate, String toDate,
	        String patientUuid, String status, String paymentModes, String cashierUuids, String timesheetUuid) {
		criteria.setFromDate(parseDate(fromDate));
		criteria.setToDate(parseDate(toDate));
		criteria.setPatientUuid(trimToNull(patientUuid));
		criteria.setStatus(parseStatus(status));
		criteria.setPaymentModes(splitCsv(paymentModes));
		criteria.setCashierUuids(splitCsv(cashierUuids));
		criteria.setTimesheetUuid(trimToNull(timesheetUuid));
	}

	private static Date parseDate(String value) {
		return StringUtils.isBlank(value) ? null : (Date) ConversionUtil.convert(value, Date.class);
	}

	private static BillStatus parseStatus(String value) {
		return StringUtils.isBlank(value) ? null : BillStatus.valueOf(value.trim().toUpperCase());
	}

	private static String trimToNull(String value) {
		return StringUtils.isBlank(value) ? null : value.trim();
	}

	private static List<String> splitCsv(String value) {
		List<String> values = new ArrayList<String>();
		if (StringUtils.isBlank(value)) {
			return values;
		}

		for (String part : value.split(",")) {
			String trimmed = trimToNull(part);
			if (trimmed != null) {
				values.add(trimmed);
			}
		}
		return values;
	}
}
