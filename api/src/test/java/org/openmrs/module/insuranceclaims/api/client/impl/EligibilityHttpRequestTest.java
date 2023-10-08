package org.openmrs.module.insuranceclaims.api.client.impl;

import org.hamcrest.Matchers;
import org.hl7.fhir.r4.model.CoverageEligibilityRequest;
import org.hl7.fhir.r4.model.CoverageEligibilityResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URISyntaxException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class EligibilityHttpRequestTest extends BaseModuleContextSensitiveTest {

    static String BASE_URL = "http://localhost:8000/api_fhir/EligibilityResponse";

    @InjectMocks
    EligibilityHttpRequestImpl request;

    @Mock
    FhirRequestClient client;

    @Test
    public void sendEligibilityRequest_shouldPassFhirObjectToClient() throws URISyntaxException {
        String expectedUrlCall = BASE_URL + "/";
        CoverageEligibilityRequest eligibilityRequest = new CoverageEligibilityRequest();
        CoverageEligibilityResponse response = new CoverageEligibilityResponse();

        when(client.postObject(eq(expectedUrlCall), eq(eligibilityRequest), eq(response.getClass())))
                .thenAnswer(i -> response);
        CoverageEligibilityResponse result = request.sendEligibilityRequest(BASE_URL, eligibilityRequest);
        Assert.assertThat(result, Matchers.samePropertyValuesAs(response));
    }
}
