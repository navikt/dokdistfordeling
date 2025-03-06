package no.nav.dokdistfordeling.itest;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import no.nav.dokdistfordeling.config.AbstractOauth2Test;
import no.nav.dokdistfordeling.config.Rdist002TestConfig;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.to.DistribuerJournalpostResponseTo;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
import static no.nav.dokdistfordeling.TestData.createDistribuerJournalpostToBuilder;
import static no.nav.dokdistfordeling.TestData.createUtenlandskAdresseTo;
import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.TvingKanal.TRYGDERETTEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(
		classes = Rdist002TestConfig.class,
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Rdist002IT extends AbstractOauth2Test {

	private static final String DISTRIBUER_JOURNALPOST_URI = "/rest/v1/distribuerjournalpost";

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
		stubAzureToken();
	}

	@Test
	public void distribuerJournalpostHappyPath() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertNotNull(qdist012ResultMessage.getStringProperty(CALL_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void distribuerJournalpostToDittNAV() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/dittNav.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertNotNull(qdist012ResultMessage.getStringProperty(CALL_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-dokumenterFraJoak-dittnav.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void shouldDistribuerJournalpostToPrintWhenTvingSentralPrintSetToTrue() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.tvingSentralPrint(true)
						.build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertNotNull(restResponse.getBestillingsId());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@ParameterizedTest
	@EnumSource(TvingKanal.class)
	public void shouldDistribuerJournalpostToKanalWhenTvingKanalIsSet(TvingKanal tvingKanal) {
		stubSafGraphQl(tvingKanal.equals(TRYGDERETTEN)? "saf/safGraphQlResponse-happy-trygderetten.json" : "saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.tvingKanal(tvingKanal.toString())
						.build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertNotNull(restResponse.getBestillingsId());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			assertThat(qdist012Result).contains(String.format("<distribusjonKanal>%s</distribusjonKanal>", tvingKanal));
		});


		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void distribuerJournalpostHappyPathMinimalAvsenderMottaker() {
		stubSafGraphQl("saf/safGraphQlResponse-happy-minimal-avsendermottaker.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoark-happy-minimal-avsendermottaker.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/rest/journalpostapi/555555555")));
	}

	@Test
	public void journalpostAlleredeDistribuertOgReturnStatusConflict() {
		stubSafGraphQl("saf/safgraphql-with-tilleggsopplysninger.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = terminateDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
		assertEquals("1ad212d2-d46d-4e73-bf6c-c1c60382da44", requireNonNull(responseEntity.getBody()).getBestillingsId());

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostAsPrintWhenMappingInPdlFailsWithAdresse() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-npid.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertNotNull(qdist012ResultMessage.getStringProperty(CALL_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoark-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void throwExceptionInDistribuerJournalpostWhenMappingInPdlFailsWithoutAdresse() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-npid.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());

		ResponseEntity<String> responseEntity = restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, POST, requestEntity, String.class);
		assertNotNull(responseEntity.getBody());
		assertTrue(responseEntity.getBody().contains("Kunne ikke hente folkeregisterident fra PDL. Respons fra PDL inneholdt ikke gjeldende folkeregisterident"));
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void distribuerJournalpostHappyPathWithDistribusjontypeIsNull() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertNotNull(restResponse.getBestillingsId());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-qdist012-input-with-null-distribusjontype.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostWithUkjentAvsenderMottakerIdHappyPath() {
		stubSafGraphQl("saf/safGraphQlResponse-TSS-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/sdp.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoark-TSS-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostWithoutAdresseHappyPath() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		stubHentMottakerOgAdresse("regoppslag/treg002-hentadresse-person-happy.json", OK.value());
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);
			assertNotNull(qdist012ResultMessage.getStringProperty(CALL_ID));

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoarkWithoutInputAdresse-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostWithUtenlandskAdresseHappyPath() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(createUtenlandskAdresseTo())
						.build(),
				createHappyPathHeaders());
		DistribuerJournalpostResponseTo restResponse = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002IT-hentDokumenterFraJoarkWithUtenlandskAdresse-happy.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void shouldDistribuerAdressetypeWithCaseInsensitiveHappy() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		putStubOppdaterJournalpost();

		DistribuerJournalpostRequestTo distribuerJournalpostRequestTo = JsonSerializer.deserialize(classpathToString("__files/rdist002/rdist002-happy-adressetype.json"), DistribuerJournalpostRequestTo.class);
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(distribuerJournalpostRequestTo, createHappyPathHeaders());
		DistribuerJournalpostResponseTo response = callDistribuerJournalpostAndAssertResponseCode(requestEntity);

		assertEquals(36, response.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			assertNotNull(qdist012ResultMessage);
			String qdist012Result = extractHentDokumenterFraJoarkXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			String qdist012ResultWithoutBestillingsId = qdist012Result.replaceAll("(<bestillingsId>)[^&]*(</bestillingsId>)", "");
			assertThat(classpathToString("__files/rdist002/rdist002-joark-hentdokumenter-utenlandskadresse.xml")).isEqualToIgnoringWhitespace(qdist012ResultWithoutBestillingsId);
		});

		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostWithoutAuthHeader() {
		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHeaderWithoutAuth());
		String restResponse = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);

		assertThat(restResponse).contains("Required header 'Authorization' is not present.");
	}

	@Test
	public void distribuerJournalpostWithoutJournalpostId() {
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.journalpostId(null)
						.build(),
				createHappyPathHeaders());
		String restResponse = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);

		assertThat(restResponse).contains("Validering av distribusjonsforespørsel feilet med feilmelding: Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=nul");
	}

	@Test
	public void distribuerJournalpostThrowsSafJournalpostQueryUnauthorizedException() {
		stubFor(post(urlMatching("/safgraphql"))
				.willReturn(aResponse()
						.withStatus(UNAUTHORIZED.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{}")));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		String restResponse = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, UNAUTHORIZED);

		assertThat(restResponse).contains("Henting av journalpost feilet med status: 401 UNAUTHORIZED");
		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	public void distribuerJournalpostThrowsSafJournalpostQueryTechnicalException() {
		stubFor(post(urlMatching("/safgraphql"))
				.willReturn(aResponse()
						.withStatus(INTERNAL_SERVER_ERROR.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		String restResponse = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, INTERNAL_SERVER_ERROR);

		assertThat(restResponse).contains("Tjenesten SAF (graphQL) feilet med status: 500 INTERNAL_SERVER_ERROR");
		verify(exactly(3), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@ParameterizedTest
	@CsvSource({
			"safgraphql-bad_request.json,Bad request,400",
			"safgraphql-not_found.json,Fant ikke journalpost,404",
			"safgraphql-unauthorized.json,Bruker er unauthorized,401",
			"safgraphql-validationerror-query.json,Feil i saf query,500",
			"safgraphql-with-variant-arkiv-jpeg.json,'Ugyldig dokumentvariant=ARKIV eller filtype=JPEG, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres',400",
			"safgraphql-with-variant-arkiv-tiff.json,'Ugyldig dokumentvariant=ARKIV eller filtype=TIFF, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres',400",
			"safgraphql-with-variant-arkiv-png.json,'Ugyldig dokumentvariant=ARKIV eller filtype=PNG, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres',400",
			"safgraphql-with-variant-original-xml-json.json, Systembruker eller saksbehandler har ikke tilgang til dokumentInfoId=666666666 og kan derfor ikke bestille distribusjon,401"
	})
	void shouldReturnCorrecteErrorTypeWhenSafRequestFails(String filename, String errorMessage, int httpErrorCode) {
		stubSafGraphQl("saf/" + filename);

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.valueOf(httpErrorCode));
		assertThat(responseEntity.getBody()).contains(errorMessage);
	}

	@Test
	public void distribuerJournalpostWithInngaaendeJournalposttype() {
		stubSafGraphQl("saf/safGraphQlResponse-inngaaendeJournalpostType.json");

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());
		String restResponse = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);

		assertThat(restResponse).contains("Feltet filtype kan ikke være null eller tomt. Fikk filtype=null");
		verify(exactly(1), postRequestedFor(urlEqualTo("/safgraphql")).withRequestBody(equalToJson(classpathToString("__files/saf/safrequest-happy.json"))));
	}

	@Test
	void shouldReturnNotFoundWhenRequestHasNoAdresseAndAdresseIsUkjentInRegoppslag() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		stubHentMottakerOgAdresse("", NOT_FOUND.value());

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);

		assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(responseEntity.getBody()).contains("Fant ikke adresseinformasjon for mottaker i PDL. Mottaker har ukjent adresse.");
	}

	@Test
	void shouldReturnGoneWhenRequestHasNoAdresseAndMottakerErDoed() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		stubHentMottakerOgAdresse("", GONE.value());

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);

		assertThat(responseEntity.getStatusCode()).isEqualTo(GONE);
		assertThat(responseEntity.getBody()).contains("Mottaker er død og har ukjent adresse.");
	}

	@Test
	void shouldReturnBadRequestWithDokProdappLengthTooLong() {
		putStubOppdaterJournalpost();

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.dokumentProdApp("ABCDEFGHIJKLMNOPQRSTU")
						.build(),
				createHappyPathHeaders());
		final ResponseEntity<String> responseEntity = callDistribuerJournalpost(requestEntity);

		assertThat(responseEntity.getStatusCode()).isEqualTo(BAD_REQUEST);
		assertThat(responseEntity.getBody()).contains("dokumentProdapp kan ikke være mer enn 20 tegn");
	}

	@Test
	void shouldReturnInternalServerErrorWhenBestemDistribusjonskanalResponseIsInvalid() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/ugyldig.json");

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());

		callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, INTERNAL_SERVER_ERROR);
	}

	@Test
	void shouldReturnBadRequestWhenBestemDistribusjonskanalResponseIsBadRequest() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal(BAD_REQUEST, "bestemdistribusjonskanal/bad_request.json");

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());

		callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);
	}

	@ParameterizedTest
	@MethodSource
	void shouldReturnInternalServerErrorWhenBestemDistribusjonskanalResponseIsUnauthorizedOrInternalServerError(HttpStatus httpStatus, String response) {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal(httpStatus, response);

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder().build(),
				createHappyPathHeaders());

		callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, INTERNAL_SERVER_ERROR);
	}

	@Test
	public void shouldReturnBadRequestIfAddressIsInvalidInRegoppslag() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		stubHentMottakerOgAdresse("regoppslag/treg002-hentadresse-person-invalid-address.json", BAD_REQUEST.value());

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());

		String body = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);
		assertThat(body).contains("Henting av adresse for bruker feilet funksjonelt mot Regoppslag. status=400 BAD_REQUEST, feilmelding=Validering av feltet postnummer feilet pga. manglende data i PDL");
	}

	@Test
	public void shouldReturnBadRequestIfAddressFromRegoppslagContainsOnlyLandkode() {
		stubSafGraphQl("saf/safGraphQlResponse-happy.json");
		stubStsToken();
		stubPdl("pdl/pdl-happy.json");
		stubBestemDistribusjonskanal("bestemdistribusjonskanal/print.json");
		stubHentMottakerOgAdresse("regoppslag/treg002-hentadresse-person-only-landkode.json", OK.value());

		HttpEntity<DistribuerJournalpostRequestTo> requestEntity = new HttpEntity<>(
				createDistribuerJournalpostToBuilder()
						.adresse(null)
						.build(),
				createHappyPathHeaders());

		String body = callDistribuerJournalpostAndAssertErrorResponseCode(requestEntity, BAD_REQUEST);
		assertThat(body).contains("Validering av distribusjonsforespørsel feilet med feilmelding: Feltet poststed kan ikke være null eller tomt. Fikk poststed=null");
	}

	private static Stream<Arguments> shouldReturnInternalServerErrorWhenBestemDistribusjonskanalResponseIsUnauthorizedOrInternalServerError() {
		return Stream.of(
				Arguments.of(UNAUTHORIZED, "bestemdistribusjonskanal/unauthorized.json"),
				Arguments.of(UNAUTHORIZED, "bestemdistribusjonskanal/internal_server_error.json")
		);
	}

	private void stubAzureToken() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubSafGraphQl(String path) {
		stubFor(post(urlMatching("/safgraphql"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	private void stubStsToken() {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("sts/stsResponse_happy.json")));
	}

	private void stubPdl(String path) {
		stubFor(post("/pdl")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	private void stubBestemDistribusjonskanal(String path) {
		stubFor(post("/rest/bestemDistribusjonskanal")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	protected static void stubBestemDistribusjonskanal(HttpStatus httpStatus, String bodyFile) {
		stubFor(post("/rest/bestemDistribusjonskanal")
				.willReturn(aResponse()
						.withStatus(httpStatus.value())
						.withHeader(CONTENT_TYPE, APPLICATION_PROBLEM_JSON_VALUE)
						.withBodyFile(bodyFile)));
	}

	private void stubHentMottakerOgAdresse(String path, int status) {
		stubFor(post(urlMatching("/regoppslag/hentMottakerOgAdresse"))
				.willReturn(aResponse()
						.withStatus(status)
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	private void putStubOppdaterJournalpost() {
		stubFor(put(urlMatching("/rest/journalpostapi/555555555"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("journalpost/oppdaterjournalpost_response.json")));
	}

	private DistribuerJournalpostResponseTo callDistribuerJournalpostAndAssertResponseCode(HttpEntity<DistribuerJournalpostRequestTo> requestEntity) {
		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, POST, requestEntity, DistribuerJournalpostResponseTo.class);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		return responseEntity.getBody();
	}

	private String callDistribuerJournalpostAndAssertErrorResponseCode(HttpEntity<DistribuerJournalpostRequestTo> requestEntity, HttpStatus expectedStatus) {
		ResponseEntity<String> responseEntity = restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, POST, requestEntity, String.class);
		assertEquals(expectedStatus, responseEntity.getStatusCode());
		return responseEntity.getBody();
	}

	private ResponseEntity<DistribuerJournalpostResponseTo> terminateDistribuerJournalpostAndAssertResponseCode(HttpEntity<DistribuerJournalpostRequestTo> requestEntity) {
		return restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, POST, requestEntity, DistribuerJournalpostResponseTo.class);
	}

	private ResponseEntity<String> callDistribuerJournalpost(HttpEntity<DistribuerJournalpostRequestTo> requestEntity) {
		return restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, POST, requestEntity, String.class);
	}

	private HttpHeaders createHappyPathHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		headers.setBearerAuth(jwt());
		return headers;
	}

	private HttpHeaders createHeaderWithoutAuth() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_JSON);
		return headers;
	}


	private String extractHentDokumenterFraJoarkXmlStringAndDecrypt(Message message) throws JMSException {
		String bestillingsId = message.getStringProperty(BESTILLINGS_ID);
		String encryptedAndMarshaledBody = ((TextMessage) message).getText();
		return new Crypto(encryptionPassphrase, bestillingsId).decrypt(encryptedAndMarshaledBody);
	}

	private String classpathToString(String classpathResource) {
		try {
			InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
			String message = IOUtils.toString(inputStream, UTF_8);
			IOUtils.closeQuietly(inputStream);
			return message;
		} catch (IOException e) {
			return "Failed to load file";
		}
	}

}
