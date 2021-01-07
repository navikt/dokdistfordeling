package no.nav.dokdistfordeling.consumer.pdl;

import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;

import no.nav.dokdistfordeling.exception.functional.PdlHentFolkeregisteridentForAktoerIdFunctionalException;
import no.nav.dokdistfordeling.exception.technical.PdlHentFolkeregisteridentForAktoerIdTechnicalException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdlGraphQLConsumerTest {

    private static final String TOKEN = "a1676cbe-daaf-4fe0-aa53-f3a7b35258d0";
    private static final String CALL_ID = "17acff54-7c80-4242-819d-20765d7c883b";

    private static RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private static RestTemplateBuilder restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
    private static StsRestConsumer stsRestConsumer = Mockito.mock(StsRestConsumer.class);

    private static PdlProperties pdlProperties;
    private static PdlGraphQLConsumer pdlGraphQLConsumer;

    @BeforeAll
    static void setup() {
        MDC.put(NAV_CALL_ID, CALL_ID);

        Mockito.when(restTemplateBuilder.setConnectTimeout(ArgumentMatchers.any())).thenReturn(restTemplateBuilder);
        Mockito.when(restTemplateBuilder.setReadTimeout(ArgumentMatchers.any())).thenReturn(restTemplateBuilder);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(stsRestConsumer.getOidcToken()).thenReturn(TOKEN);

        pdlProperties = new PdlProperties();
        pdlProperties.setUrl("http://localhost");
        pdlGraphQLConsumer = new PdlGraphQLConsumer(restTemplateBuilder, stsRestConsumer, pdlProperties);
    }

    @BeforeEach
    void beforeEach() {
        Mockito.reset(restTemplate);
    }

    @AfterAll
    static void tearDown(){
        MDC.clear();
    }

    @Test
    void hentAktoerIdForPersonnummerHappy() throws URISyntaxException {

        PdlHentIdenterResponse pdlHentIdenterResponse = createPdlResponse(
                null,
                createIdent("1000012345678", false, IdentType.FOLKEREGISTERIDENT)
        );

        Mockito.when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(PdlHentIdenterResponse.class))).thenReturn(new ResponseEntity<>(pdlHentIdenterResponse, HttpStatus.OK));

        String returnValue = pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId("123");

        assertEquals("1000012345678", returnValue);
        Mockito.verify(restTemplate).exchange(
                ArgumentMatchers.argThat(new RequestEntityMatcher(
                        "123",
                        CALL_ID,
                        TOKEN,
                        new URI(pdlProperties.getUrl())
                )),
                ArgumentMatchers.eq(PdlHentIdenterResponse.class)
        );
    }

    @Test
    void shouldThrowFunctionalExceptionIfResponseContainsError() throws URISyntaxException {
        PdlHentIdenterResponse pdlHentIdenterResponse = createPdlResponse(
                List.of(createError("ErrorMessage", "ErrorCode", "ErrorClassification")),
                createIdent("1000012345678", false, IdentType.FOLKEREGISTERIDENT)
        );

        Mockito.when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(PdlHentIdenterResponse.class))).thenReturn(new ResponseEntity<>(pdlHentIdenterResponse, HttpStatus.OK));

        assertThrows(PdlHentFolkeregisteridentForAktoerIdFunctionalException.class, () -> {
            pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId("123");
        });

        Mockito.verify(restTemplate).exchange(
                ArgumentMatchers.argThat(new RequestEntityMatcher(
                        "123",
                        CALL_ID,
                        TOKEN,
                        new URI(pdlProperties.getUrl())
                )),
                ArgumentMatchers.eq(PdlHentIdenterResponse.class)
        );
    }

    @Test
    void shouldThrowFunctionalExceptionIfResponseHas4xxStatusCode() throws URISyntaxException {

        Mockito.when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(PdlHentIdenterResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        assertThrows(PdlHentFolkeregisteridentForAktoerIdFunctionalException.class, () -> {
            pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId("123");
        });

        Mockito.verify(restTemplate).exchange(
                ArgumentMatchers.argThat(new RequestEntityMatcher(
                        "123",
                        CALL_ID,
                        TOKEN,
                        new URI(pdlProperties.getUrl())
                )),
                ArgumentMatchers.eq(PdlHentIdenterResponse.class)
        );
    }

    @Test
    void shouldThrowTechnicalExceptionIfResponseHas5xxStatusCode() throws URISyntaxException {

        Mockito.when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(PdlHentIdenterResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(PdlHentFolkeregisteridentForAktoerIdTechnicalException.class, () -> {
            pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId("123");
        });

        Mockito.verify(restTemplate).exchange(
                ArgumentMatchers.argThat(new RequestEntityMatcher(
                        "123",
                        CALL_ID,
                        TOKEN,
                        new URI(pdlProperties.getUrl())
                )),
                ArgumentMatchers.eq(PdlHentIdenterResponse.class)
        );
    }

    @Test
    void shouldThrowFunctionalExceptionIfResponseContainsNoCurrentFolkeregisterident() throws URISyntaxException {
        PdlHentIdenterResponse pdlHentIdenterResponse = createPdlResponse(
                null,
                createIdent("1000012345678", false, IdentType.AKTORID),
                createIdent("1000012345678", false, IdentType.NPID),
                createIdent("1000012345678", true, IdentType.FOLKEREGISTERIDENT)
        );

        Mockito.when(restTemplate.exchange(ArgumentMatchers.any(), ArgumentMatchers.eq(PdlHentIdenterResponse.class))).thenReturn(new ResponseEntity<>(pdlHentIdenterResponse, HttpStatus.OK));
        assertThrows(PdlHentFolkeregisteridentForAktoerIdFunctionalException.class, () -> {
            pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId("123");
        });

        Mockito.verify(restTemplate).exchange(
                ArgumentMatchers.argThat(new RequestEntityMatcher(
                        "123",
                        CALL_ID,
                        TOKEN,
                        new URI(pdlProperties.getUrl())
                )),
                ArgumentMatchers.eq(PdlHentIdenterResponse.class)
        );
    }

    private PdlHentIdenterResponse.PdlIdentTo createIdent(String ident, boolean historisk, IdentType identType) {
        PdlHentIdenterResponse.PdlIdentTo pdlIdentTo = new PdlHentIdenterResponse.PdlIdentTo();
        pdlIdentTo.setIdent(ident);
        pdlIdentTo.setHistorisk(historisk);
        pdlIdentTo.setGruppe(identType);

        return pdlIdentTo;
    }

    private PdlHentIdenterResponse.PdlErrorTo createError(String message, String code, String classification) {

        PdlHentIdenterResponse.PdlErrorTo pdlErrorTo = new PdlHentIdenterResponse.PdlErrorTo();
        PdlHentIdenterResponse.PdlErrorExtensionTo pdlErrorExtensionTo = new PdlHentIdenterResponse.PdlErrorExtensionTo();

        pdlErrorExtensionTo.setCode(code);
        pdlErrorExtensionTo.setClassification(classification);

        pdlErrorTo.setMessage(message);
        pdlErrorTo.setExtensions(pdlErrorExtensionTo);

        return pdlErrorTo;
    }

    private PdlHentIdenterResponse createPdlResponse(List<PdlHentIdenterResponse.PdlErrorTo> errors, PdlHentIdenterResponse.PdlIdentTo ...identTos) {

        PdlHentIdenterResponse pdlHentIdenterResponse = new PdlHentIdenterResponse();
        PdlHentIdenterResponse.PdlHentIdenterData pdlHentIdenterData = new PdlHentIdenterResponse.PdlHentIdenterData();
        PdlHentIdenterResponse.PdlIdenter pdlIdenter = new PdlHentIdenterResponse.PdlIdenter();

        pdlIdenter.setIdenter(List.of(identTos));
        pdlHentIdenterData.setHentIdenter(pdlIdenter);
        pdlHentIdenterResponse.setData(pdlHentIdenterData);
        pdlHentIdenterResponse.setErrors(errors);

        return pdlHentIdenterResponse;
    }
}
