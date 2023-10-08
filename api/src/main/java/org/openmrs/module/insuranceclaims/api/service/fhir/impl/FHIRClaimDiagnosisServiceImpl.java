package org.openmrs.module.insuranceclaims.api.service.fhir.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.exceptions.FHIRException;
import org.openmrs.Concept;
import org.openmrs.api.context.Context;
// import org.openmrs.module.fhir.api.util.BaseOpenMRSDataUtil;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.insuranceclaims.api.model.InsuranceClaim;
import org.openmrs.module.insuranceclaims.api.model.InsuranceClaimDiagnosis;
import org.openmrs.module.insuranceclaims.api.service.db.DiagnosisDbService;
import org.openmrs.module.insuranceclaims.api.service.fhir.FHIRClaimDiagnosisService;
import org.openmrs.module.insuranceclaims.api.service.fhir.util.InsuranceClaimConstants;
import org.openmrs.module.fhir2.api.translators.impl.ConceptTranslatorImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.openmrs.module.insuranceclaims.api.service.fhir.util.IdentifierUtil.getUnambiguousElement;

public class FHIRClaimDiagnosisServiceImpl implements FHIRClaimDiagnosisService {

    private DiagnosisDbService diagnosisDbService;

    @Override
    public Claim.DiagnosisComponent generateClaimDiagnosisComponent(InsuranceClaimDiagnosis omrsClaimDiagnosis) {
        Claim.DiagnosisComponent newDiagnosis = new Claim.DiagnosisComponent();
        ConceptTranslatorImpl conceptTranslatorImpl = new ConceptTranslatorImpl();
        Concept diagnosisConcepts = omrsClaimDiagnosis.getConcept();
        CodeableConcept diagnosis = conceptTranslatorImpl.toFhirResource(diagnosisConcepts);
        String uuid = omrsClaimDiagnosis.getUuid();
        newDiagnosis.setId(uuid.contains("/") ? uuid.substring(uuid.indexOf("/") + 1) : uuid);
        newDiagnosis.setDiagnosis(diagnosis);

        return newDiagnosis;
    }

    @Override
    public List<Claim.DiagnosisComponent> generateClaimDiagnosisComponent(
            List<InsuranceClaimDiagnosis> omrsClaimDiagnosis) {
        List<Claim.DiagnosisComponent> allDiagnosisComponents = new ArrayList<>();

        for (InsuranceClaimDiagnosis insuranceClaimDiagnosis : omrsClaimDiagnosis) {
            Claim.DiagnosisComponent nextDiagnosis = generateClaimDiagnosisComponent(insuranceClaimDiagnosis);
            allDiagnosisComponents.add(nextDiagnosis);
        }
        return allDiagnosisComponents;
    }

    @Override
    public List<Claim.DiagnosisComponent> generateClaimDiagnosisComponent(InsuranceClaim omrsInsuranceClaim)
    throws FHIRException {
        List<InsuranceClaimDiagnosis> claimDiagnoses =
                diagnosisDbService.findInsuranceClaimDiagnosis(omrsInsuranceClaim.getId());
        List<Claim.DiagnosisComponent> fhirDiagnosisComponent = generateClaimDiagnosisComponent(claimDiagnoses);
        addCodingToDiagnosis(fhirDiagnosisComponent);
        return fhirDiagnosisComponent;
    }

    @Override
    public InsuranceClaimDiagnosis createOmrsClaimDiagnosis(
            Claim.DiagnosisComponent claimDiagnosis, List<String> errors) {
        InsuranceClaimDiagnosis diagnosis = new InsuranceClaimDiagnosis();
        // BaseOpenMRSDataUtil.readBaseExtensionFields(diagnosis, claimDiagnosis);

        if (claimDiagnosis.getId() != null) {
            String id = claimDiagnosis.getId();
            diagnosis.setUuid(id.contains("/") ? id.substring(id.indexOf("/") + 1) : id);
        }
        try {
            validateDiagnosisCodingSystem(claimDiagnosis);
            diagnosis.setConcept(getConceptByCodeableConcept(claimDiagnosis.getDiagnosisCodeableConcept(), errors));
        } catch (FHIRException e) {
            errors.add(e.getMessage());
        }

        return diagnosis;
    }

    public void setDiagnosisDbService(DiagnosisDbService diagnosisDao) {
        this.diagnosisDbService = diagnosisDao;
    }

    private void addCodingToDiagnosis(List<Claim.DiagnosisComponent>  diagnosisComponents) throws FHIRException {
        for (Claim.DiagnosisComponent diagnosis: diagnosisComponents) {
            setDiagnosisPrimaryCoding(diagnosis);
            List<Coding> coding = diagnosis.getDiagnosisCodeableConcept().getCoding();
            List<CodeableConcept> diagnosisType = coding.stream()
                    .map(Coding::getSystem)
                    .map(systemName -> new CodeableConcept().setText(systemName))
                    .collect(Collectors.toList());
            diagnosis.setType(diagnosisType);
        }
    }

    private void setDiagnosisPrimaryCoding(Claim.DiagnosisComponent diagnosis) throws FHIRException {
        String primaryCoding = Context.getAdministrationService().getGlobalProperty(InsuranceClaimConstants.PRIMARY_DIAGNOSIS_MAPPING);
        List<Coding> diagnosisCoding = diagnosis.getDiagnosisCodeableConcept().getCoding();

        for (Coding c : diagnosisCoding) {
            if (c.getSystem().equals(primaryCoding)) {
                Collections.swap(diagnosisCoding, 0, diagnosisCoding.indexOf(c));
                break;
            }
        }
    }

    private void validateDiagnosisCodingSystem(Claim.DiagnosisComponent diagnosis) throws FHIRException {
        //Method assigns diagnosis system (ICD-10, CIEL, etc.) from type to the concept coding if it don't have assigned system
        if (CollectionUtils.isEmpty(diagnosis.getType())) {
            return;
        }
        String codingSystem = diagnosis.getTypeFirstRep().getText();
        CodeableConcept concept = diagnosis.getDiagnosisCodeableConcept();
        for (Coding coding : concept.getCoding()) {
            if (coding.getSystem() == null) {
                coding.setSystem(codingSystem);
            }
        }
    }

    private Concept getConceptByCodeableConcept(CodeableConcept codeableConcept, List<String> errors) {
        List<Coding> diagnosisCoding = codeableConcept.getCoding();
        List<Concept> concept = getConceptByFHIRCoding(diagnosisCoding);

        Concept uniqueConcept = getUnambiguousElement(concept);
        if (uniqueConcept == null) {
            errors.add("No matching concept found for the given codings: \n" + diagnosisCoding);
            return null;
        } else {
            return uniqueConcept;
        }
    }

    private List<Concept> getConceptByFHIRCoding(List<Coding> coding) {
        return coding.stream()
                .map(this::getConceptByFHIRCoding)
                .collect(Collectors.toList());
    }

    private Concept getConceptByFHIRCoding(Coding coding) {
        String conceptCode = coding.getCode();
        String systemName = coding.getSystem();

        Concept concept = null;
        if (FhirConstants.OPENMRS_CODE_SYSTEM_PREFIX.equals(systemName)) {
            concept = Context.getConceptService().getConceptByUuid(conceptCode);
        } else { if (StringUtils.isNotEmpty(systemName)) {
                List<Concept> concepts =  Context.getConceptService().getConceptsByMapping(conceptCode, systemName);
                concept = getUnambiguousElement(concepts);
            }
        }
        return concept;
    }
}
