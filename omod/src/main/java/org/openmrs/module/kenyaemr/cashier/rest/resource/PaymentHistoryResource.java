package org.openmrs.module.kenyaemr.cashier.rest.resource;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.cashier.api.IBillService;
import org.openmrs.module.kenyaemr.cashier.api.model.HistorySearchCriteria;
import org.openmrs.module.kenyaemr.cashier.api.model.PaymentHistorySummary;
import org.openmrs.module.kenyaemr.cashier.base.resource.AlreadyPagedWithLength;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.kenyaemr.cashier.rest.support.HistoryCriteriaBuilder;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE + "/payment-history",
        supportedClass = PaymentHistorySummary.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class PaymentHistoryResource extends DelegatingCrudResource<PaymentHistorySummary> {

	@Override
	public PaymentHistorySummary newDelegate() {
		return new PaymentHistorySummary();
	}

	@Override
	public PaymentHistorySummary save(PaymentHistorySummary delegate) {
		throw new ResourceDoesNotSupportOperationException();
	}

	@Override
	public PaymentHistorySummary getByUniqueId(String uuid) {
		return getService().getPaymentHistoryByUuid(uuid, new HistorySearchCriteria());
	}

	@Override
	protected AlreadyPaged<PaymentHistorySummary> doGetAll(RequestContext context) {
		return buildPagedResult(context);
	}

	@Override
	protected AlreadyPaged<PaymentHistorySummary> doSearch(RequestContext context) {
		return buildPagedResult(context);
	}

	private AlreadyPaged<PaymentHistorySummary> buildPagedResult(RequestContext context) {
		HistorySearchCriteria criteria = HistoryCriteriaBuilder.fromRequestContext(context);
		long totalCount = getService().getPaymentHistoryCount(criteria);
		List<PaymentHistorySummary> results = getService().getPaymentHistory(criteria);
		boolean hasMore = hasMoreResults(criteria, results.size(), totalCount);
		return new AlreadyPagedWithLength<PaymentHistorySummary>(context, results, hasMore, totalCount);
	}

	private boolean hasMoreResults(HistorySearchCriteria criteria, int pageSize, long totalCount) {
		int startIndex = criteria.getStartIndex() == null ? 0 : criteria.getStartIndex().intValue();
		return startIndex + pageSize < totalCount;
	}

	@Override
	public void delete(PaymentHistorySummary delegate, String reason, RequestContext context) {
		throw new ResourceDoesNotSupportOperationException();
	}

	@Override
	public void purge(PaymentHistorySummary delegate, RequestContext context) {
		throw new ResourceDoesNotSupportOperationException();
	}

	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("display");

		if (!(rep instanceof RefRepresentation)) {
			description.addProperty("billUuid");
			description.addProperty("patientUuid");
			description.addProperty("patientName");
			description.addProperty("identifier");
			description.addProperty("invoiceId");
			description.addProperty("paymentDate");
			description.addProperty("paymentAmount");
			description.addProperty("paymentMethod");
			description.addProperty("referenceId");
		}

		description.addSelfLink();
		return description;
	}

	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		throw new ResourceDoesNotSupportOperationException();
	}

	@Override
	public DelegatingResourceDescription getUpdatableProperties() {
		throw new ResourceDoesNotSupportOperationException();
	}

	@PropertyGetter("display")
	public String getDisplay(PaymentHistorySummary summary) {
		if (summary == null) {
			return "";
		}

		return StringUtils.defaultIfBlank(summary.getInvoiceId(), "--") + " | "
		        + StringUtils.defaultIfBlank(summary.getPatientName(), "--");
	}

	private IBillService getService() {
		return Context.getService(IBillService.class);
	}
}
