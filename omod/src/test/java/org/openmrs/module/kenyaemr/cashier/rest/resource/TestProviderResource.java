package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.Provider;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_9.ProviderResource1_9;

@Resource(name = "v1/provider", supportedClass = Provider.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class TestProviderResource extends ProviderResource1_9 {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		return description;
	}
}
