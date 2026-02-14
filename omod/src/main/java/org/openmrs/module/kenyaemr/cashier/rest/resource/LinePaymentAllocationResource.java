package org.openmrs.module.kenyaemr.cashier.rest.resource;

import java.math.BigDecimal;

import org.openmrs.module.kenyaemr.cashier.api.model.LinePaymentAllocation;
import org.openmrs.module.kenyaemr.cashier.rest.controller.base.CashierResourceController;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;

@Resource(name = RestConstants.VERSION_1 + CashierResourceController.KENYAEMR_CASHIER_NAMESPACE
        + "/linePaymentAllocation", supportedClass = LinePaymentAllocation.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class LinePaymentAllocationResource extends DelegatingCrudResource<LinePaymentAllocation> {
    @Override
    public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
        DelegatingResourceDescription description = new DelegatingResourceDescription();
        description.addProperty("uuid");

        if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
            description.addProperty("billLineItem", Representation.REF);
            description.addProperty("allocatedAmount");
            description.addProperty("dateCreated");
            description.addProperty("voided");
        }

        return description;
    }

    @PropertyGetter("allocatedAmount")
    public BigDecimal getAllocatedAmount(LinePaymentAllocation instance) {
        return instance.getAllocatedAmount();
    }

    @PropertySetter("allocatedAmount")
    public void setAllocatedAmount(LinePaymentAllocation instance, Object amount) {
        if (amount instanceof Integer) {
            instance.setAllocatedAmount(BigDecimal.valueOf((Integer) amount));
        } else if (amount instanceof Double) {
            instance.setAllocatedAmount(BigDecimal.valueOf((Double) amount));
        } else if (amount instanceof BigDecimal) {
            instance.setAllocatedAmount((BigDecimal) amount);
        }
    }

    @Override
    public LinePaymentAllocation getByUniqueId(String uniqueId) {
        return null;
    }

    @Override
    protected void delete(LinePaymentAllocation delegate, String reason, RequestContext context) {
        throw new UnsupportedOperationException("Allocations are managed automatically by the bill.");
    }

    @Override
    public LinePaymentAllocation newDelegate() {
        return new LinePaymentAllocation();
    }

    @Override
    public LinePaymentAllocation save(LinePaymentAllocation delegate) {
        throw new UnsupportedOperationException("Allocations are managed automatically by the bill.");
    }

    @Override
    public void purge(LinePaymentAllocation delegate, RequestContext context) {
        throw new UnsupportedOperationException("Allocations are managed automatically by the bill.");
    }

    @Override
    public NeedsPaging<LinePaymentAllocation> doGetAll(RequestContext context) {
        throw new UnsupportedOperationException("Allocations are accessed through the bill API.");
    }

}
