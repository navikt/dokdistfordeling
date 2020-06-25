package no.nav.dokdistfordeling.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistfordeling.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.DistribuerJournalpostResponseTo;
import no.nav.dokdistfordeling.config.Rdist002TestConfig;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.util.MappingUtil;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.UnitTestUtil.ARKIV_SYSTEM;
import static no.nav.dokdistfordeling.UnitTestUtil.DOK_INFO_ID_1;
import static no.nav.dokdistfordeling.UnitTestUtil.DOK_INFO_ID_2;
import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {Rdist002TestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Rdist002IT {

    private static final String OIDC_TOKEN = "eyAidHlwIjogIkpXVCIsICJraWQiOiAiMWwySmtDb1RMMTBibWVBeHlsZzR4Umk4ajJZPSIsICJhbGciOiAiUlMyNTYiIH0.eyAiYXRfaGFzaCI6ICJ4RklSS0dpTWZ4ZFVPS3c0ZmQ4MW9BIiwgInN1YiI6ICJaOTkyMzEwIiwgImF1ZGl0VHJhY2tpbmdJZCI6ICJiZDdlYWE0ZC1mYzIzLTQ2ZGMtOGRjZi1iMjJmNzU1NDExZjQtMjAyMDc5MzQiLCAiaXNzIjogImh0dHBzOi8vaXNzby1xLmFkZW8ubm86NDQzL2lzc28vb2F1dGgyIiwgInRva2VuTmFtZSI6ICJpZF90b2tlbiIsICJhdWQiOiAiaWRhLXEiLCAiY19oYXNoIjogInctbGx3ZlJMenVpRFBselpkY1BhenciLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIyZmNlNWU1ZS02ODdjLTQ5ZmYtOTRjYS1jNzE2OGVmY2M2MmQiLCAiYXpwIjogImlkYS1xIiwgImF1dGhfdGltZSI6IDE1NTUwNzQ3NjcsICJyZWFsbSI6ICIvIiwgImV4cCI6IDE1NTUwNzgzNjcsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAiaWF0IjogMTU1NTA3NDc2NyB9.orrUotLp8SMkCpigVhkAUlw9Rx5tigBrYNVv3j8fTmkIe-I1MEI0xctxM-tnLbrgcW3I-3Ye_bkS4KplhR4spnG9hT45L1dD-yoLsu8R6cD1PklMsx8m93XmaTHDReGZAI3uKO4KSPcQHyVE7-tIc6CWYqbVXWmEUxUsHNYm3bWO_0rZ-Su6CWVCEBz3yWa85rUcPn0Il-_BWkgF-0YhOWJn3ndKAl_96ARmR-nllhUnQDYqHk2DwYLWnz_WOb4HuuqxKRP5i1h8zHwGIR6VORCzWgFViiFNTPT54Mtr2fZtVinP8W70JoRZ1pKbk-bYK4ErJgACU8npdGBZYTZa6g";
    private static final String DISTRIBUER_JOURNALPOST_URI = "/rest/v1/distribuerjournalpost";
    private static final String JOURNALPOST_ID = "555555555";
    private static final String NAV_CONSUMER_ID = "itest";

    private static final String BATCHID = "66666";
    private static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
    private static final String ADRESSETYPE_NORSK = "norskPostadresse";
    private static final String ADRESSETYPE_UTENLANDSK = "utenlandskPostadresse";
    private static final String ADRESSELINJE1 = "eksempelveien 23 A";
    private static final String ADRESSELINJE2 = "eksempelveien 24 A";
    private static final String ADRESSELINJE3 = "eksempelveien 25 A";
    private static final String POSTSTED = "poststed";
    private static final String POSTNUMMER = "1337";
    private static final String LAND_NO = "NO";
    private static final String LAND_US = "US";
    private static final String DOKUMENTPRODAPP = "dokumentprodapp";

    private static final String DOKUMENTTYPEID = "000001";
    private static final String TEMA = "OPP";
    private static final String MOTTAKER_ID = "***gammelt_fnr***";
    private static final String MOTTAKER_NAVN = "Jan Neimansen";
    private static final String BRUKER_ID = "***gammelt_fnr***";
    @Inject
    protected TestRestTemplate restTemplate;
    private @Value("${hentdokumenter_fra_joark_crypto_password}")
    String encryptionPassphrase;
    @Inject
    private JmsTemplate jmsTemplate;
    @Inject
    private Queue qdist012;

    @BeforeEach
    public void setupBefore() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.removeAllMappings();
    }

    @Test
    public void distribuerJournalpostHappyPath() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("dokkat/tkat020-happy.json")));

        final String callId = UUID.randomUUID().toString();
        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

        assertEquals(36, restResponse.getBestillingsId().length());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
            String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
            assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
            assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));

            assertNotNull(qdist012Result);
            String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
            assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
        });

        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostWithUkjentAvsenderMottakerIdHappyPath() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-TSS-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("dokkat/tkat020-happy.json")));

        final String callId = UUID.randomUUID().toString();
        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

        assertEquals(36, restResponse.getBestillingsId().length());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
            String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
            assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
            assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));

            assertNotNull(qdist012Result);
            String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
            assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoark-TSS-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
        });

        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }


    @Test
    public void distribuerJournalpostWithoutAdresseHappyPath() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("dokkat/tkat020-happy.json")));

        stubFor(post(urlMatching("/regoppslag/hentMottakerOgAdresse")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("regoppslag/treg002-hentadresse-person-happy.json")));

        stubFor(get(urlMatching("/stsRest/samltoken")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("reststs/reststs-saml-happy.json")));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().adresse(null).build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

        assertEquals(36, restResponse.getBestillingsId().length());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
            String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
            assertNotNull(qdist012ResultMessage.getStringProperty(CALL_ID));

            assertNotNull(qdist012Result);
            String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
            assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoarkWithoutInputAdresse-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
        });

        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostWithUtenlandskAdresseHappyPath() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("dokkat/tkat020-happy.json")));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().adresse(createUtenlandskAdresse()).build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

        assertEquals(36, restResponse.getBestillingsId().length());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
            String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

            assertNotNull(qdist012Result);
            String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
            assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoarkWithUtenlandskAdresse-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
        });

        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void shouldDistribuerAdressetypeWithCaseInsensitiveHappyily() {

        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .withBodyFile("dokkat/tkat020-happy.json")));

        DistribuerJournalpostRequestTo distribuerJournalpostRequestTo = MappingUtil.jsonStringToObject(classpathToString("__files/rdist002/rdist002-happy-adressetype.json"), DistribuerJournalpostRequestTo.class);
        HttpEntity requestEntity = new HttpEntity<>(distribuerJournalpostRequestTo, createHappyPathHeaders());
        DistribuerJournalpostResponseTo response = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

        assertEquals(36, response.getBestillingsId().length());

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
            String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

            assertNotNull(qdist012Result);
            String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
            assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoarkWithUtenlandskAdresse-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
        });

        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostWithoutAuthHeader() {
        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHeaderWithoutAuth());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

        assertNull(restResponse.getBestillingsId());
    }

    @Test
    public void distribuerJournalpostWithoutJournalpostId() {
        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().journalpostId(null).build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

        assertNull(restResponse.getBestillingsId());
    }

    @Test
    public void distribuerJournalpostThrowsSafJournalpostIkkeFunnetFunctionalException() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.NOT_FOUND);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostThrowsSafJournalpostQueryUnauthorizedException() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBody("{}")));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.UNAUTHORIZED);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostThrowsSafJournalpostQueryTechnicalException() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.INTERNAL_SERVER_ERROR);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(3), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    public void distribuerJournalpostWithInngaaendeJournalposttype() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-inngaaendeJournalpostType.json")));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    @Disabled
    public void distribuerJournalpostThrowsDokkatGetDokumenttypeInfoFunctionalException() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    @Test
    @Disabled
    public void distribuerJournalpostThrowsDokkatGetDokumenttypeInfoTechnicalException() {
        stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
                .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBodyFile("saf/safGraphQlResponse-happy.json")));

        stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));

        HttpEntity requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo().build(), createHappyPathHeaders());
        DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.INTERNAL_SERVER_ERROR);

        assertNull(restResponse.getBestillingsId());
        verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
        verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
    }

    private void assertQdist012Result(Distribusjonbestilling qdist012Result, String restResponseBestillingsId) {
        assertNotNull(qdist012Result);
        assertEquals(restResponseBestillingsId, qdist012Result.getBestillingsId());

        assertEquals(BATCHID, qdist012Result.getBatchId());
        assertEquals(BESTILLENDEFAGSYSTEM, qdist012Result.getBestillendeFagsystem());
        assertEquals(TEMA, qdist012Result.getTema());
        assertArkivInformasjon(qdist012Result.getArkivInformasjon());
        assertMottaker((Person) qdist012Result.getMottaker());
        assertBruker((AktoerId) qdist012Result.getBruker());
        assertEquals(DOKUMENTPRODAPP, qdist012Result.getDokumentProdApp());
        assertDokumenter(qdist012Result.getDokumenter());
    }

    private void assertArkivInformasjon(ArkivInformasjon arkivInformasjon) {
        assertEquals(JOURNALPOST_ID, arkivInformasjon.getArkivId());
        assertEquals(ARKIV_SYSTEM, arkivInformasjon.getArkivSystem());
    }

    private void assertMottaker(Person person) {
        assertEquals(MOTTAKER_ID, person.getPersonidentifikator());
        assertEquals(MOTTAKER_NAVN, person.getNavn());
    }

    private void assertBruker(AktoerId aktoer) {
        assertEquals(BRUKER_ID, aktoer.getAktoerId());
        assertNull(aktoer.getNavn());
    }

    private void assertNorskPostadresse(NorskPostadresse adresse) {
        assertNotNull(adresse);
        assertEquals(LAND_NO, adresse.getLand());
        assertEquals(POSTNUMMER, adresse.getPostnummer());
        assertEquals(POSTSTED, adresse.getPoststed());
        assertEquals(ADRESSELINJE1, adresse.getAdresselinje1());
        assertEquals(ADRESSELINJE2, adresse.getAdresselinje2());
        assertEquals(ADRESSELINJE3, adresse.getAdresselinje3());
    }

    private void assertUtenlandskPostadresse(UtenlandskPostadresse adresse) {
        assertNotNull(adresse);
        assertEquals(LAND_US, adresse.getLand());
        assertEquals(ADRESSELINJE1, adresse.getAdresselinje1());
        assertEquals(ADRESSELINJE2, adresse.getAdresselinje2());
        assertEquals(ADRESSELINJE3, adresse.getAdresselinje3());
    }

    private void assertDokumenter(List<DokumentInformasjon> dokumenter) {
        assertThat(dokumenter.size()).isGreaterThan(0);

        dokumenter.forEach(dokumentInformasjon -> {
            if (HOVEDDOKUMENT.name().equals(dokumentInformasjon.getTilknyttetSom())) {
                assertEquals(dokumentInformasjon.getRekkefolge(), 1);
                assertEquals(SLADDET, dokumentInformasjon.getVariantFormat());
                assertEquals(DOK_INFO_ID_1, dokumentInformasjon.getArkivDokumentInfoId());
            } else {
                assertThat(dokumentInformasjon.getRekkefolge()).isGreaterThan(1);
                assertEquals(VEDLEGG.name(), dokumentInformasjon.getTilknyttetSom());
                assertEquals(ARKIV, dokumentInformasjon.getVariantFormat());
                assertEquals(DOK_INFO_ID_2, dokumentInformasjon.getArkivDokumentInfoId());
            }
            assertEquals(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID, dokumentInformasjon.getDokumenttypeId());
        });
    }


    private DistribuerJournalpostResponseTo callDistribuerJournalpostAndAssertResponseCode(HttpEntity requestEntity, HttpStatus expectedStatus) {
        ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = this.restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, requestEntity, DistribuerJournalpostResponseTo.class);
        assertEquals(expectedStatus, responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    private HttpHeaders createHappyPathHeaders() {
        return createHappyPathHeaders(null);
    }


    private HttpHeaders createHappyPathHeaders(String callId) {
        return createHappyPathHeaders(callId, null);
    }

    private HttpHeaders createHappyPathHeaders(String callId, String consumerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + OIDC_TOKEN);
        if (callId != null) {
            headers.add("Nav-CallId", callId);
        }
        if (consumerId != null) {
            headers.add("Nav-Consumer-Id", consumerId);
        }
        return headers;
    }

    private HttpHeaders createHeaderWithoutAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        return headers;
    }

    private DistribuerJournalpostRequestTo.DistribuerJournalpostRequestToBuilder createHappyPathDistribuerJournalpostRequestTo() {
        return DistribuerJournalpostRequestTo.builder()
                .journalpostId(JOURNALPOST_ID)
                .batchId(BATCHID)
                .bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
                .adresse(new DistribuerJournalpostRequestTo.AdresseTo(
                        ADRESSETYPE_NORSK,
                        POSTNUMMER,
                        POSTSTED,
                        ADRESSELINJE1,
                        ADRESSELINJE2,
                        ADRESSELINJE3,
                        LAND_NO
                ))
                .dokumentProdApp(DOKUMENTPRODAPP);

    }

    private DistribuerJournalpostRequestTo.AdresseTo createUtenlandskAdresse() {
        return new DistribuerJournalpostRequestTo.AdresseTo(
                ADRESSETYPE_UTENLANDSK,
                null,
                null,
                ADRESSELINJE1,
                ADRESSELINJE2,
                ADRESSELINJE3,
                LAND_US
        );
    }

    private String extractHentDokumenterFraJoarkXmlStringAndDecrypt(Message message) throws JMSException, JAXBException {
        String bestillingsId = message.getStringProperty(BESTILLINGS_ID);
        String encryptedAndMarshaledBody = ((TextMessage) message).getText();
        return new Crypto(encryptionPassphrase, bestillingsId).decrypt(encryptedAndMarshaledBody);
    }

    private String classpathToString(String classpathResource) {
        String message = null;
        try {
            InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
            message = IOUtils.toString(inputStream, UTF_8);
            IOUtils.closeQuietly(inputStream);
        } catch (IOException e) {
            return "Failed to load file";
        }
        return message;
    }

}
