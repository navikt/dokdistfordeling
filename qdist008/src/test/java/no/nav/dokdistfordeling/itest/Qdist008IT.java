package no.nav.dokdistfordeling.itest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistfordeling.itest.config.Qdist008ItestConfig;
import no.nav.dokdistfordeling.qdist008.Qdist008Route;
import no.nav.dokdistfordeling.storage.BucketStorage;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.DOKMET_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {Qdist008ItestConfig.class},
		webEnvironment = RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist008IT {
	private static final String DOKUMENTTYPE_ID = "1111111";

	private static final String PDL_URL = "/pdl";
	private static final String DOKMET_URL = "/rest/dokumenttypeinfo/" + DOKUMENTTYPE_ID;
	private static final String DOKDISTADMIN_URL = "/rest/v1/administrerforsendelse";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";
	private static final String OPPDATERDISTRIBUSJONSINFO_URL = "/rest/journalpostapi/1234/oppdaterDistribusjonsinfo";
	private static final String SAF_GRAPHQL_URL = "/saf/graphql";

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist008;

	@Autowired
	private Queue qdist008FunksjonellFeil;

	@Autowired
	private Queue qdist008Bq;

	@Autowired
	private Queue qdist009;

	@Autowired
	private Queue qdist010;

	@Autowired
	private Queue qdist011;

	@Autowired
	private Queue qdist013;

	@Autowired
	private Queue qdist015;

	@Autowired
	private Queue qdist016;

	@Autowired
	private BucketStorage bucketStorage;

	@Autowired
	public CacheManager cacheManager;

	@Autowired
	protected CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	protected RetryRegistry retryRegistry;

	@BeforeEach
	public void setupBefore() {
		cacheManager.getCache(DOKMET_CACHE).clear();
		circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);

		reset(bucketStorage);
		when(bucketStorage.exists(anyString())).thenReturn(true);

		stubFor(post(SAF_GRAPHQL_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("saf/safGraphQlResponse-happy.json")));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalPrint() throws Exception {
		stubNaisTexasToken();
		stubPostPdl();
		stubAzure();
		stubGetDokumenttypeInfo();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlMatching(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalDittNav() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_kanal_dittnav_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist010);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoNAV_NOHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilDittNavOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseWithUtsendingskanalSDP() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_sdp_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist011);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
			verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
			verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSDPHappy.json"))));
			verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilSDPOutputHappy.json"))));
			verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void shouldProcessForsendelseWithUtsendingskanalTrygderetten() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_trygderetten_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist013);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoTRYGDERETTENHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilTrygderettenOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseWithUtsendingskanalDPO() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_dpo_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist015);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), postRequestedFor(urlPathMatching(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelse_til_dpo_happy.json"))));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfo_dpo_happy.json"))));
	}

	@Test
	public void shouldDistribuereForsendelseTilDPV() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_til_dpvt.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist016);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelse_til_dpv_happy.json"))));
	}

	@Test
	public void shouldProcessForsendelseWithUtsendingskanalLokalPrint() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPatchOppdaterDistribusjonsinfo();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_lokalprint_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
			verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
			verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoLHappy.json"))));
			verify(exactly(0), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilLokalPrintOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse/oppdaterforsendelse")));
		});
	}

	@Test
	public void shouldProcessForsendelseWithUtsendingskanalIngenDistribusjon() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPatchOppdaterDistribusjonsinfo();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_ingendistribusjon_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
			verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
			verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoINGEN_DISTRIBUSJONHappy.json"))));
			verify(exactly(0), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilIngenDistribusjonOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse/oppdaterforsendelse")));
		});
	}

	@Test
	public void shouldProcessForsendelseOnlyRequiredInputFieds() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse()
						.withStatus(OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_only_required_fields_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});
	}

	@Test
	public void shouldProcessWithoutContactingDokmet() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse()
						.withStatus(OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_har_tittel_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(0), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseWithTittelHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseWithoutContactingPdl() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostRdist001();
		stubPutOppdaterForsendelse();
		stubPatchOppdaterDistribusjonsinfo();
		stubNaisTexasToken();
		stubAzure();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_pdl_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(0), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseWithOrganisasjonOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldPassOnCallId() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubPutOppdaterForsendelse();

		final String callId = UUID.randomUUID().toString();
		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_trygderetten_happypath.xml"), callId);

		await().atMost(10, SECONDS).untilAsserted(() -> {
			TextMessage responseTextMsg = receiveTextMessage(qdist013);
			assertEquals(callId, responseTextMsg.getStringProperty(CALL_ID));
		});
	}

	@Test
	public void shouldThrowForsendelseMapperException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_arkivsystemkode.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_arkivsystemkode.xml"));
		});
	}

	@Test
	public void shouldThrowManglerHoveddokumentValidationException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));
		});
	}

	@Test
	public void shouldThrowExceptionIfTemaErNullorEmpty() throws Exception {
		stubPostPdl();
		stubNaisTexasToken();
		stubAzure();
		stubPatchOppdaterDistribusjonsinfo();
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseEmptyTema.json")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_invalid_tema.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
		});
	}

	@Test
	public void shouldThrowSamhandlerUtenAddresseValidationException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_samhandler_uten_addresse.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_samhandler_uten_addresse.xml"));
		});
	}

	@Test
	public void shouldThrowValidationExceptionWhenOnlyForsendelseMetadataIsSet() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_dpo_sett_kun_forsendelseMetadata.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_dpo_sett_kun_forsendelseMetadata.xml"));
		});
	}

	@Test
	public void shouldThrowInvalidUuidException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Test
	public void shouldThrowNotAvailableInBucketValidationException() throws Exception {
		stubNaisTexasToken();
		stubAzure();
		when(bucketStorage.exists(anyString())).thenReturn(false);

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});
	}

	@Test
	public void shouldThrowDokmetTechnicalException() throws Exception {
		stubNaisTexasToken();
		stubAzure();
		stubFor(get(DOKMET_URL)
				.willReturn(aResponse()
						.withStatus(INTERNAL_SERVER_ERROR.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(3), getRequestedFor(urlEqualTo(DOKMET_URL)));
	}

	@Test
	public void shouldThrowPdlFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubFor(post(PDL_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-fail.json")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
	}

	@Test
	public void shouldThrowJournalpostFeilregistrertException() throws Exception {
		ListAppender<ILoggingEvent> listAppender = setupAndReturnListAppender();

		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubSafGraphQl("saf/safGraphQlResponse-FEILREGISTRERT.json");

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertTrue(listAppender.list.stream().map(ILoggingEvent::getMessage).toList()
						.contains("Forkaster melding på qdist008 grunnet=no.nav.dokdistfordeling.exception.functional.JournalpostFeilregistrertException: journalpostId=1234 er feilregistrert og distribusjon av bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 avbrytes"))
		);

		verify(exactly(1), postRequestedFor(urlEqualTo(SAF_GRAPHQL_URL)));
	}

	@Test
	public void shouldThrowJournalpostErAlleredeEkspedertException() throws Exception {
		ListAppender<ILoggingEvent> listAppender = setupAndReturnListAppender();

		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubSafGraphQl("saf/safGraphQlResponse-EKSPEDERT.json");

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() ->
				assertTrue(listAppender.list.stream().map(ILoggingEvent::getMessage).toList()
						.contains("Forkaster melding på qdist008 grunnet=no.nav.dokdistfordeling.exception.functional.JournalpostErAlleredeDistribuertException: journalpostId=1234 er allerede distribuert og distribusjon av bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 avbrytes"))
		);

		verify(exactly(1), postRequestedFor(urlEqualTo(SAF_GRAPHQL_URL)));
	}

	@Test
	public void shouldThrowValidationExceptionForJournalpostUnderArbeid() throws Exception {
		ListAppender<ILoggingEvent> listAppender = setupAndReturnListAppender();

		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubSafGraphQl("saf/safGraphQlResponse-UNDER_ARBEID.json");

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", ""));
			assertTrue(listAppender.list.stream().map(ILoggingEvent::getMessage).toList()
					.contains("Legger melding på funksjonell backoutkø for qdist008 for bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 og forsendelseId= grunnet=no.nav.dokdistfordeling.exception.functional.ValidationException: journalpostId=1234 har ugyldig status=UNDER_ARBEID og distribusjon av bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 avbrytes"));
		});

		verify(exactly(1), postRequestedFor(urlEqualTo(SAF_GRAPHQL_URL)));
	}

	@Test
	public void shouldThrowPdlTechicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubFor(post(PDL_URL)
				.willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())
						.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertNotNull(resultOnQdist008BackoutQueue);
			assertEquals(resultOnQdist008BackoutQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(3), postRequestedFor(urlEqualTo(PDL_URL)));
	}

	@Test
	public void shouldThrowBestemDokdistKanalFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", ""));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
	}

	@Test
	public void shouldThrowBestemDokdistKanalTechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_magleradresse_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_magleradresse_happypath.xml"));
		});
	}

	@Test
	public void shouldThrowBestemDokdistKanalMappingException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
	}

	@Test
	public void shouldThrowOpprettForsendelseFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();
		stubFor(post(DOKDISTADMIN_URL)
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
	}


	@Test
	public void shouldThrowOpprettForsendelseTechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();
		stubFor(post(DOKDISTADMIN_URL)
				.willReturn(aResponse()
						.withStatus(INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(3), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
	}

	@Test
	public void shouldThrowJournalpostAPITechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();
		stubFor(patch(urlMatching(format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(3), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
	}

	@Test
	public void shouldPutMessageOnFunksjonellBoqWhenBadRequestFromDokdistadmin() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse()
						.withStatus(BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), postRequestedFor(urlEqualTo(PDL_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo((OPPDATERFORSENDELSE_URL))));
	}

	@Test
	public void shouldPutMessageOnBoqWhenInternalServerErrorFromDokdistadmin() throws Exception {
		stubGetDokumenttypeInfo();
		stubNaisTexasToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();
		stubPatchOppdaterDistribusjonsinfo();
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse()
						.withStatus(INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(DOKMET_URL)));
		verify(exactly(1), patchRequestedFor(urlPathMatching(OPPDATERDISTRIBUSJONSINFO_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rdist001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(3), putRequestedFor(urlEqualTo((OPPDATERFORSENDELSE_URL))));
	}

	private void stubPostRdist001() {
		stubFor(post("/rest/v1/administrerforsendelse")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
	}

	private void stubPostPdl() {
		stubFor(post(PDL_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("pdl/pdl-happy.json")));
	}
	
	void stubNaisTexasToken() {
		stubFor(post("/nais-texas")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("nais-texas/token_response.json")));
	}

	void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubGetDokumenttypeInfo() {
		stubFor(get(DOKMET_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("dokmet/dokumenttypeInfo-happy.json")));
	}

	private void stubPatchOppdaterDistribusjonsinfo() {
		stubFor(patch(urlMatching(format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubPutOppdaterForsendelse() {
		stubFor(put("/rest/v1/administrerforsendelse/oppdaterforsendelse")
				.withRequestBody(containing("{\"forsendelseId\":33333,\"forsendelseStatus\":\"KLAR_FOR_DIST\"}"))
				.willReturn(aResponse()
						.withStatus(OK.value())));
	}

	private void stubSafGraphQl(String bodyFile) {
		stubFor(post(SAF_GRAPHQL_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyFile)));
	}

	private void sendStringMessage(Queue queue, final String message) {
		sendStringMessage(queue, message, null);
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = session.createTextMessage();
			msg.setText(message);
			if (callId != null) {
				msg.setStringProperty(CALL_ID, callId);
			}
			return msg;
		});
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.close(inputStream);
		return message;
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Queue queue) {
		Object response = jmsTemplate.receiveAndConvert(queue);
		if (response instanceof JAXBElement) {
			response = ((JAXBElement<?>) response).getValue();
		}
		return (T) response;
	}

	protected TextMessage receiveTextMessage(final Queue queue) {
		return (TextMessage) jmsTemplate.receive(queue);
	}

	private String getRequestAsJson(String filename) throws IOException {
		return IOUtils.toString(requireNonNull(this.getClass().getResourceAsStream("/" + filename)), UTF_8);
	}

	private ListAppender<ILoggingEvent> setupAndReturnListAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(Qdist008Route.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);
		return listAppender;
	}

}