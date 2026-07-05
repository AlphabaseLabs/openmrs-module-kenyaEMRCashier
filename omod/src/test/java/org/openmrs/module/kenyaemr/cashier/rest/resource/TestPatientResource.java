package org.openmrs.module.kenyaemr.cashier.rest.resource;

import org.openmrs.Patient;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.v1_0.resource.openmrs1_8.PatientResource1_8;

@Resource(name = "v1/patient", supportedClass = Patient.class, supportedOpenmrsVersions = { "2.0 - 2.*" })
public class TestPatientResource extends PatientResource1_8 {
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		return description;
	}
}
