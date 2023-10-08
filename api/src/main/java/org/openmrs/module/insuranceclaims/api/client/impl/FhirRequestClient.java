package org.openmrs.module.insuranceclaims.api.client.impl;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.api.context.Context;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.SimpleRequestHeaderInterceptor;
import org.openmrs.module.fhir2.api.FhirClientService;
import org.openmrs.module.insuranceclaims.api.client.FHIRClient;
import org.openmrs.module.insuranceclaims.api.client.FhirMessageConventer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openmrs.module.insuranceclaims.api.client.ClientConstants.API_LOGIN_PROPERTY;
import static org.openmrs.module.insuranceclaims.api.client.ClientConstants.API_PASSWORD_PROPERTY;
import static org.openmrs.module.insuranceclaims.api.client.ClientConstants.CLIENT_HELPER_USER_AGENT;
import static org.openmrs.module.insuranceclaims.api.client.ClientConstants.USER_AGENT;

public class FhirRequestClient implements FHIRClient {

    private RestTemplate restTemplate = new RestTemplate();

    private FhirClientService fhirClientService;

    private HttpHeaders headers = new HttpHeaders();

    private AbstractHttpMessageConverter<IBaseResource> conventer = new FhirMessageConventer();

    public <T extends IBaseResource> T getObject(String url, Class<T> objectClass) throws URISyntaxException {
        prepareRestTemplate();
        setRequestHeaders();
        IGenericClient client = fhirClientService.getClientForR4(url);
        ResponseEntity<T> response = sendRequest(new ClientHttpEntity(client, headers, HttpMethod.GET, null), objectClass);
        return response.getBody();
    }

    public <T,K extends IBaseResource> K postObject(String url, T object, Class<K> objectClass) throws URISyntaxException,
            HttpServerErrorException {
        prepareRestTemplate();
        setRequestHeaders();
        ClientHttpEntity clientHttpEntity = createPostClientHttpEntity(url, object);
        ResponseEntity<K> response = sendRequest(clientHttpEntity, objectClass);
        return response.getBody();
    }

    private <L> ResponseEntity<L> sendRequest(ClientHttpEntity clientHttpEntity, Class<L> objectClass)  {
        HttpEntity entity = new HttpEntity(clientHttpEntity.getBody(), headers);
        return restTemplate.exchange(clientHttpEntity.getUrl(), clientHttpEntity.getMethod(), entity, objectClass);
    }

    private void setRequestHeaders() {
        headers = new HttpHeaders();
        String username = Context.getAdministrationService().getGlobalProperty(API_LOGIN_PROPERTY);
        String password = Context.getAdministrationService().getGlobalProperty(API_PASSWORD_PROPERTY);
        AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();
        interceptor.addHeaderValue("Authorization", new BasicAuthInterceptor(username, password).toString());
        interceptor.addHeaderValue("Accept", "application/json");
        // for (IClientInterceptor interceptor : Arrays.asList(new BasicAuthInterceptor(username, password),
        // new SimpleRequestHeaderInterceptor("Accept", "application/json"));) {
        //     interceptor.addToHeaders(headers);
        // }

        headers.add(USER_AGENT, CLIENT_HELPER_USER_AGENT);
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private <L> ClientHttpEntity createPostClientHttpEntity(String url, L object) throws URISyntaxException {
        // ClientHttpEntity clientHttpEntity = fhirClientHelper.createRequest(url, object);
        IParser parser = FhirContext.forR4().newJsonParser();
		ClientHttpEntity clientHttpEntity = ClientHttpEntity();
        clientHttpEntity.setMethod(HttpMethod.POST);
        clientHttpEntity.setUrl(new URI(url));
        return clientHttpEntity;
    }

    private void prepareRestTemplate() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>(getCustomMessageConverter());
        converters.add(conventer);
        restTemplate.setMessageConverters(converters);
    }

	private List<HttpMessageConverter<?>> getCustomMessageConverter() {
		return Arrays.asList(new HttpMessageConverter<?>[]
				{ new FhirMessageConventer(), new StringHttpMessageConverter() });
	}
}
