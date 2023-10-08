package org.openmrs.module.insuranceclaims.api.service.fhir.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.impl.PatientReferenceTranslatorImpl;
import org.openmrs.module.insuranceclaims.api.model.InsuranceClaim;

import java.util.List;

import static org.openmrs.module.insuranceclaims.api.service.fhir.util.InsuranceClaimConstants.PATIENT_EXTERNAL_ID_IDENTIFIER_UUID;

public final class PatientUtil {

    public static Reference buildPatientReference(InsuranceClaim claim) {
        Patient patient = claim.getPatient();
        PatientReferenceTranslatorImpl patientReferenceTranslatorImpl  = new PatientReferenceTranslatorImpl();
        Reference patientReference = patientReferenceTranslatorImpl.toFhirResource(patient);

        String patientId = patient.getActiveIdentifiers()
                .stream()
                .filter(c -> c.getIdentifierType().getUuid().equals(PATIENT_EXTERNAL_ID_IDENTIFIER_UUID))
                .findFirst()
                .map(PatientIdentifier::getIdentifier)
                .orElse(patient.getUuid());

        String reference = FhirConstants.PATIENT + "/" + patientId;
        patientReference.setReference(reference);

        return patientReference;
    }

    public static boolean isSamePatient(Patient omrsPatient, org.hl7.fhir.r4.model.Patient fhirPatient) {
        String fhirGivenName = fhirPatient.getNameFirstRep().getGivenAsSingleString();
        String fhirFamilyName = fhirPatient.getNameFirstRep().getFamily();
        return omrsPatient.getGivenName().equals(fhirGivenName)
                && omrsPatient.getFamilyName().equals(fhirFamilyName)
                && omrsPatient.getBirthdate().compareTo(fhirPatient.getBirthDate()) == 0;
    }

    public static boolean isPatientInList(Patient omrsPatient, List<org.hl7.fhir.r4.model.Patient> fhirPatients) {
        boolean isInList = false;
        for (org.hl7.fhir.r4.model.Patient fhirPatient: fhirPatients) {
            isInList = isInList || isSamePatient(omrsPatient, fhirPatient);
        }
        return isInList;
    }


    public static Patient getPatientById(String personId) {
        if (NumberUtils.isNumber(personId)) {
            int parsedId = Integer.parseInt(personId);
            return Context.getPatientService().getPatient(parsedId);
        } else {
            return Context.getPatientService().getPatientByUuid(personId);
        }
    }

    private PatientUtil() {}
}
