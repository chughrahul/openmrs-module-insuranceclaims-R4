package org.openmrs.module.insuranceclaims.api.service.fhir.impl;

import org.hl7.fhir.r4.model.CoverageEligibilityRequest;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.insuranceclaims.api.service.fhir.FHIREligibilityService;

import static org.openmrs.module.insuranceclaims.api.service.fhir.util.IdentifierUtil.buildReference;

public class FHIREligibilityServiceImpl implements FHIREligibilityService {

    @Override
    public CoverageEligibilityRequest generateEligibilityRequest(String policyId) {
        CoverageEligibilityRequest request = new CoverageEligibilityRequest();
        Reference patient = buildReference(FhirConstants.PATIENT, policyId);
        request.setPatient(patient);

        return request;
    }
}