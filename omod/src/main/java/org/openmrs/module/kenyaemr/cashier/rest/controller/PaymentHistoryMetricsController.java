package org.openmrs.module.kenyaemr.cashier.rest.controller;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.HistorySearchCriteria;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentHistoryMetricsSummary;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentMethodTotalSummary;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.support.HistoryCriteriaBuilder;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
        + "/metrics/payment-history")
public class PaymentHistoryMetricsController {

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public SimpleObject getPaymentHistoryMetrics(@RequestParam(value = "fromDate", required = false) String fromDate,
	        @RequestParam(value = "toDate", required = false) String toDate,
	        @RequestParam(value = "patientUuid", required = false) String patientUuid,
	        @RequestParam(value = "status", required = false) String status,
	        @RequestParam(value = "paymentModes", required = false) String paymentModes,
	        @RequestParam(value = "cashierUuids", required = false) String cashierUuids,
	        @RequestParam(value = "timesheetUuid", required = false) String timesheetUuid) {
		HistorySearchCriteria criteria = HistoryCriteriaBuilder.fromRequestParams(fromDate, toDate, patientUuid, status,
		    paymentModes, cashierUuids, timesheetUuid);
		PaymentHistoryMetricsSummary metrics = getService().getPaymentHistoryMetrics(criteria);

		SimpleObject result = new SimpleObject();
		result.put("totalPayments", metrics.getTotalPayments());
		result.put("cash", metrics.getCash());
		result.put("others", metrics.getOthers());
		result.put("topPayeeName", metrics.getTopPayeeName());
		result.put("topPayeeAmount", metrics.getTopPayeeAmount());
		result.put("paymentMethodTotals", toSimplePaymentMethodTotals(metrics.getPaymentMethodTotals()));
		return result;
	}

	private List<SimpleObject> toSimplePaymentMethodTotals(List<PaymentMethodTotalSummary> totals) {
		List<SimpleObject> results = new ArrayList<SimpleObject>();
		for (PaymentMethodTotalSummary total : totals) {
			SimpleObject item = new SimpleObject();
			item.put("paymentMethod", total.getPaymentMethod());
			item.put("total", total.getTotal());
			results.add(item);
		}
		return results;
	}

	private IBillService getService() {
		return Context.getService(IBillService.class);
	}
}
