package org.openmrs.module.kenyaemr.cashier.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistorySearchCriteria {
	private Date fromDate;
	private Date toDate;
	private String patientUuid;
	private BillStatus status;
	private List<String> paymentModes = new ArrayList<String>();
	private List<String> cashierUuids = new ArrayList<String>();
	private String timesheetUuid;
	private Integer limit;
	private Integer startIndex;

	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	public String getPatientUuid() {
		return patientUuid;
	}

	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}

	public BillStatus getStatus() {
		return status;
	}

	public void setStatus(BillStatus status) {
		this.status = status;
	}

	public List<String> getPaymentModes() {
		return paymentModes;
	}

	public void setPaymentModes(List<String> paymentModes) {
		this.paymentModes = defaultList(paymentModes);
	}

	public List<String> getCashierUuids() {
		return cashierUuids;
	}

	public void setCashierUuids(List<String> cashierUuids) {
		this.cashierUuids = defaultList(cashierUuids);
	}

	public String getTimesheetUuid() {
		return timesheetUuid;
	}

	public void setTimesheetUuid(String timesheetUuid) {
		this.timesheetUuid = timesheetUuid;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(Integer startIndex) {
		this.startIndex = startIndex;
	}

	public boolean hasPaymentModes() {
		return paymentModes != null && !paymentModes.isEmpty();
	}

	public boolean hasCashierUuids() {
		return cashierUuids != null && !cashierUuids.isEmpty();
	}

	private static <T> List<T> defaultList(List<T> values) {
		return values == null ? new ArrayList<T>() : values;
	}
}
