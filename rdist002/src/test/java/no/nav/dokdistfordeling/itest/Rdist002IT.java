package no.nav.dokdistfordeling.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBException;
import no.nav.dokdistfordeling.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.DistribuerJournalpostResponseTo;
import no.nav.dokdistfordeling.config.Rdist002TestConfig;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.util.MappingUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VIKTIG;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

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
	private static final String LAND_KOSOVO = "XK";
	private static final String DOKUMENTPRODAPP = "dokumentprodapp";

	private static final String DOKUMENTTYPEID = "000001";

	@Autowired
	protected TestRestTemplate restTemplate;
	private @Value("${hentdokumenter_fra_joark_crypto_password}")
	String encryptionPassphrase;
	@Autowired
	private JmsTemplate jmsTemplate;
	@Autowired
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
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
			assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void shouldDistribuerJournalpostToPrintWhenTvingSentralPrintSetToTrue() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));


		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.tvingSentralPrint(true)
				.build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
			assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void distribuerJournalpostHappyPathMinimalAvsenderMottaker() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy-minimal-avsendermottaker.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal")
				.withRequestBody(containing("\"mottakerId\":\"0\""))
				.withRequestBody(containing("\"mottakerType\":\"SAMHANDLER_UKJENT\""))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
						.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
			assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));
			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002IT-hentDokumenterFraJoark-happy-minimal-avsendermottaker.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);


		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void journalpostAlleredeDistribuertOgReturnStatusConflict() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safgraphql-with-tilleggsopplysninger.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = terminateDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
		assertEquals("1ad212d2-d46d-4e73-bf6c-c1c60382da44", requireNonNull(responseEntity.getBody()).getBestillingsId());

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostAsPrintWhenMappingInPdlFailsWithAdresse() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-npid.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
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
	public void throwExceptionInDistribuerJournalpostWhenMappingInPdlFailsWithoutAdresse() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-npid.json")));

		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestIngenAdresseTo()
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build(), createHappyPathHeaders(UUID.randomUUID().toString(), NAV_CONSUMER_ID));

		ResponseEntity<String> responseEntity = restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, requestEntity, String.class);
		assertTrue(responseEntity.getBody().contains("Kunne ikke hente folkeregisterident fra PDL. Respons fra PDL inneholdt ikke gjeldende folkeregisterident"));
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void distribuerJournalpostHappyPathWithDistribusjontypeIsNull() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.OK);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertEquals(callId, qdist012ResultMessage.getStringProperty(CALL_ID));
			assertEquals(NAV_CONSUMER_ID, qdist012ResultMessage.getStringProperty(CONSUMER_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002IT-qdist012-input-with-null-distribusjontype.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
	}

	@Test
	public void distribuerJournalpostWithUkjentAvsenderMottakerIdHappyPath() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-TSS-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalSDP.json")));

		putStubOppdaterJournalpost();

		final String callId = UUID.randomUUID().toString();
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHappyPathHeaders(callId, NAV_CONSUMER_ID));
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
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		stubFor(post(urlMatching("/regoppslag/hentMottakerOgAdresse")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("regoppslag/treg002-hentadresse-person-happy.json")));

		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).adresse(null).build(), createHappyPathHeaders());
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
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse())
						.adresse(createUtenlandskAdresse(LAND_US))
						.build(), createHappyPathHeaders());
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
	public void shouldDistribuerAdressetypeWithCaseInsensitiveHappy() {

		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		putStubOppdaterJournalpost();

		DistribuerJournalpostRequestTo distribuerJournalpostRequestTo = MappingUtil.jsonStringToObject(classpathToString("__files/rdist002/rdist002-happy-adressetype.json"), DistribuerJournalpostRequestTo.class);
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(distribuerJournalpostRequestTo, createHappyPathHeaders());
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
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHeaderWithoutAuth());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

		assertNull(restResponse.getBestillingsId());
	}

	@Test
	public void distribuerJournalpostWithoutJournalpostId() {
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).journalpostId(null).build(), createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

		assertNull(restResponse.getBestillingsId());
	}

	@Test
	public void distribuerJournalpostThrowsSafJournalpostQueryUnauthorizedException() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.UNAUTHORIZED.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBody("{}")));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.UNAUTHORIZED);

		assertNull(restResponse.getBestillingsId());
		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
	}

	@Test
	public void distribuerJournalpostThrowsSafJournalpostQueryTechnicalException() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.INTERNAL_SERVER_ERROR);

		assertNull(restResponse.getBestillingsId());
		verify(exactly(3), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
	}

	@ParameterizedTest
	@CsvSource({
			"safgraphql-bad_request.json,Bad request,400",
			"safgraphql-not_found.json,Fant ikke journalpost,404",
			"safgraphql-unauthorized.json,Bruker er unauthorized,401",
			"safgraphql-validationerror-query.json,Feil i saf query,500",
	})
	void shouldReturnCorrecteErrorTypeWhenSafRequestFails(String filename, String errorMessage, int httpErrorCode) {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/" + filename)));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).adresse(null).build(), createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.valueOf(httpErrorCode));
		assertThat(responseEntity.getBody()).contains(errorMessage);
	}

	@Test
	public void distribuerJournalpostWithInngaaendeJournalposttype() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-inngaaendeJournalpostType.json")));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).build(), createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity, HttpStatus.BAD_REQUEST);

		assertNull(restResponse.getBestillingsId());
		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPEID)));
	}

	@Test
	void shouldReturnNotFoundWhenRequestHasNoAdresseAndAdresseIsUkjentInRegoppslag() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		stubFor(post(urlMatching("/regoppslag/hentMottakerOgAdresse")).willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBody("")));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).adresse(null).build(), createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody()).contains("Fant ikke adresseinformasjon for mottaker i PDL. Mottaker har ukjent adresse.");
	}

	@Test
	void shouldReturnGoneWhenRequestHasNoAdresseAndMottakerErDoed() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("sts/stsResponse_happy.json")));

		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("pdl/pdl-happy.json")));

		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));

		stubFor(post(urlMatching("/regoppslag/hentMottakerOgAdresse")).willReturn(aResponse().withStatus(GONE.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
				.withBody("")));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).adresse(null).build(), createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);
		assertThat(responseEntity.getStatusCode()).isEqualTo(GONE);
		assertThat(responseEntity.getBody()).contains("Mottaker er død og har ukjent adresse.");
	}

	@Test
	void shouldReturnBadRequestWithDokProdappLengthTooLong() {
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(createHappyPathDistribuerJournalpostRequestTo(createNorskAdresse()).dokumentProdApp("ABCDEFGHIJKLMNOPQRSTU").build(), createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody()).contains("dokumentProdapp kan ikke være mer enn 20 tegn");
	}

	private void putStubOppdaterJournalpost() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
		stubFor(put(urlMatching("/rest/journalpostapi/555555555"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value()).withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("journalpost/oppdaterjournalpost_response.json")));
	}

	private DistribuerJournalpostResponseTo callDistribuerJournalpostAndAssertResponseCode(HttpEntity<DistribuerJournalpostRequestTo> requestEntity, HttpStatus expectedStatus) {
		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, requestEntity, DistribuerJournalpostResponseTo.class);
		assertEquals(expectedStatus, responseEntity.getStatusCode());
		return responseEntity.getBody();
	}

	private ResponseEntity<DistribuerJournalpostResponseTo> terminateDistribuerJournalpostAndAssertResponseCode(HttpEntity<DistribuerJournalpostRequestTo> requestEntity) {
		return restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, requestEntity, DistribuerJournalpostResponseTo.class);
	}

	private ResponseEntity<String> callDistribuerJournalpost(HttpEntity<DistribuerJournalpostRequestTo> requestEntity) {
		return restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, requestEntity, String.class);
	}

	private HttpHeaders createHappyPathHeaders() {
		return createHappyPathHeaders(null);
	}


	private HttpHeaders createHappyPathHeaders(String callId) {
		return createHappyPathHeaders(callId, null);
	}

	private HttpHeaders createHappyPathHeaders(String callId, String consumerId) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
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
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	private DistribuerJournalpostRequestTo.DistribuerJournalpostRequestToBuilder createHappyPathDistribuerJournalpostRequestTo(DistribuerJournalpostRequestTo.AdresseTo adresse) {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCHID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(adresse)
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.dokumentProdApp(DOKUMENTPRODAPP);

	}

	private DistribuerJournalpostRequestTo.DistribuerJournalpostRequestToBuilder createHappyPathDistribuerJournalpostRequestIngenAdresseTo() {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCHID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(null)
				.dokumentProdApp(DOKUMENTPRODAPP);

	}

	private DistribuerJournalpostRequestTo.AdresseTo createUtenlandskAdresse(String landkode) {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_UTENLANDSK,
				null,
				null,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				landkode
		);
	}

	private DistribuerJournalpostRequestTo.AdresseTo createNorskAdresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				POSTNUMMER,
				POSTSTED,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_NO
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
