package no.nav.dokdistfordeling.consumer.pdl;

import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;

import java.net.URI;

import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;

public class RequestEntityMatcher implements ArgumentMatcher<RequestEntity> {

    private String personnummer;
    private String callId;
    private String token;
    private URI pdlUrl;


    RequestEntityMatcher(String personnummer, String callId, String token, URI pdlUrl){
        this.personnummer = personnummer;
        this.callId = callId;
        this.token = token;
        this.pdlUrl = pdlUrl;
    }


    @Override
    public boolean matches(RequestEntity right) {
        HttpHeaders headers = right.getHeaders();
        HttpMethod method = right.getMethod();
        URI uri = right.getUrl();
        PdlRequest body = (PdlRequest) right.getBody();

        return  //header
            (headers.size() == 5) &&
            (headers.getContentType().equals(MediaType.APPLICATION_JSON)) &&
            (headers.getAccept().size() == 1) &&
            (headers.getAccept().get(0).equals(MediaType.APPLICATION_JSON)) &&
            (headers.get(NAV_CALL_ID).size() == 1) &&
            (headers.get(NAV_CALL_ID).get(0).equals(callId)) &&
            (headers.get(HttpHeaders.AUTHORIZATION).size() == 1) &&
            (headers.get(HttpHeaders.AUTHORIZATION).get(0).equals(BEARER_PREFIX + token)) &&
            (headers.get("Nav-Consumer-Token").size() == 1) &&
            (headers.get("Nav-Consumer-Token").get(0).equals(BEARER_PREFIX + token)) &&
            // method
            method.equals(HttpMethod.POST) &&
            // URI
            uri.equals(pdlUrl) &&
            //body
            (body.getVariables().size() == 1) &&
            body.getVariables().get("ident").equals(personnummer);
    }
}