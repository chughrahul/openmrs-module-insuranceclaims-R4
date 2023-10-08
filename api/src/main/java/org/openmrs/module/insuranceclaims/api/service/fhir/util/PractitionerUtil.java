package org.openmrs.module.insuranceclaims.api.service.fhir.util;

import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.module.fhir2.FhirConstants;
// import org.openmrs.module.fhir.api.util.FHIRUtils;
import org.openmrs.module.fhir2.api.translators.impl.PractitionerReferenceTranslatorProviderImpl;
import org.openmrs.module.insuranceclaims.api.model.InsuranceClaim;

import static org.openmrs.module.insuranceclaims.api.service.fhir.util.InsuranceClaimConstants.PROVIDER_EXTERNAL_ID_ATTRIBUTE_UUID;

public final class PractitionerUtil {
    public static Reference buildPractitionerReference(InsuranceClaim claim) {
        Provider provider = claim.getProvider();
        PractitionerReferenceTranslatorProviderImpl practitionerReferenceTranslatorProviderImpl = new PractitionerReferenceTranslatorProviderImpl();
        Reference pracitionerReference = practitionerReferenceTranslatorProviderImpl.toFhirResource(provider);

        String providerId = provider.getActiveAttributes()
                .stream()
                .filter(c -> c.getAttributeType().getUuid().equals(PROVIDER_EXTERNAL_ID_ATTRIBUTE_UUID))
                .findFirst()
                .map(ProviderAttribute::getValueReference)
                .orElse(provider.getUuid());

        String reference = FhirConstants.PRACTITIONER + "/" + providerId;

        pracitionerReference.setReference(reference);

        return pracitionerReference;
    }

    private PractitionerUtil() {}
}
