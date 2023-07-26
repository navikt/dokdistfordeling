package no.nav.dokdistfordeling.itest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBElement;
import no.nav.dokdistfordeling.itest.config.Qdist008ItestConfig;
import no.nav.dokdistfordeling.qdist008.Qdist008Route;
import no.nav.dokdistfordeling.storage.BucketStorage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {Qdist008ItestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist008IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENTTYPE_ID = "1111111";
	private static final String STSSTRING = "/stsRest/token?grant_type=client_credentials&scope=openid";

	private static final String DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL = "/rest/v1/administrerforsendelse";
	private static final String OPPDATERFORSENDELSE_URL = "/rest/v1/administrerforsendelse/oppdaterforsendelse";

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
	private Queue qdist016;

	@Autowired
	private BucketStorage bucketStorage;

	@Autowired
	public CacheManager cacheManager;


	@BeforeEach
	public void setupBefore() {
		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(bucketStorage);
		when(bucketStorage.exists(anyString())).thenReturn(true);

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();

		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalPrint() throws Exception {
		stubSTSToken();
		stubPostPdl();
		stubAzure();
		stubGetDokumenttypeInfo();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						 .withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalDittNav() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_kanal_dittnav_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist010);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoNAV_NOHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilDittNavOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalSDP() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));


		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_sdp_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist011);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
			verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSDPHappy.json"))));
			verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilSDPOutputHappy.json"))));
			verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
		});
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalTrygderetten() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_tregeretten_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist013);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoTRYGDERETTENHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilTrygderettenOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldDistribuereForsendelseTilDPV() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_til_dpvt.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist016);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelse_til_dpv_happy.json"))));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalLokalPrint() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_localprint_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
			verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoLHappy.json"))));
			verify(exactly(0), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilLokalPrintOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse/oppdaterforsendelse")));
		});
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalIngenDistribusjon() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));


		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_ingendistribusjon_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
			verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoINGEN_DISTRIBUSJONHappy.json"))));
			verify(exactly(0), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilIngenDistribusjonOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse//oppdaterforsendelse")));
		});
	}

	@Test
	public void shouldProcessForsendelseOnlyRequiredInputFieds() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_only_required_fields_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

	}

	@Test
	public void shouldProcessWithoutContactingDokkat() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_har_tittel_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithTittelAvoidDokkatHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldProcessForsendelseWithoutContactingPdl() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));
		stubSTSToken();
		stubAzure();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_pdl_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response).isEqualToIgnoringWhitespace(classpathToString("out/out-happy.txt"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(1), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(0), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithOrganisasjonOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(OPPDATERFORSENDELSE_URL)));
	}

	@Test
	public void shouldPassOnCallId() throws Exception {
		stubGetDokumenttypeInfo();
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));


		final String callId = UUID.randomUUID().toString();
		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_tregeretten_happypath.xml"), callId);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMsg = receiveTextMessage(qdist013);
			assertEquals(callId, responseTextMsg.getStringProperty(CALL_ID));
		});
	}

	@Test
	public void shouldThrowForsendelseMapperException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_arkivsystemkode.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_arkivsystemkode.xml"));
		});
	}

	@Test
	public void shouldThrowManglerHoveddokumentValidationException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));
		});
	}

	@Test
	public void shoulThrwoExceptionIfTemaErNullorEmpty() throws Exception {
		stubPostPdl();
		stubSTSToken();
		stubAzure();
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseEmptyTema.json")));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));


		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_invalid_tema.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
		});

	}

	@Test
	public void shouldThrowSamhandlerUtenAddresseValidationException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_samhandler_uten_addresse.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_samhandler_uten_addresse.xml"));
		});
	}

	@Test
	public void shouldThrowInvalidUuidException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Test
	public void shouldThrowNotAvailableInBucketValidationException() throws Exception {
		stubSTSToken();
		stubAzure();
		when(bucketStorage.exists(anyString())).thenReturn(false);
		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});
	}

	@Test
	public void shouldThrowDokkatTechnicalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));
		stubSTSToken();
		stubAzure();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
	}

	@Test
	public void shouldThrowPdlFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-fail.json")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowJournalpostFeilregistrertException() throws Exception {
		WireMock.removeAllMappings();

		Logger logger = (Logger) LoggerFactory.getLogger(Qdist008Route.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-FEILREGISTRERT.json")));
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));


		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			assertTrue(listAppender.list.stream().map(ILoggingEvent::getMessage).collect(Collectors.toList()).contains("no.nav.dokdistfordeling.exception.functional.JournalpostFeilregistrertException: journalpostId=1234 er feilregistrert og distribusjon av bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 avbrytes;"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
	}

	@Test
	public void shouldThrowValidationExceptionForJournalpostUnderArbeid() throws Exception {
		WireMock.removeAllMappings();

		Logger logger = (Logger) LoggerFactory.getLogger(Qdist008Route.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);

		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-UNDER_ARBEID.json")));
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", ""));
			assertTrue(listAppender.list.stream().map(ILoggingEvent::getMessage).collect(Collectors.toList()).contains("no.nav.dokdistfordeling.exception.functional.ValidationException: journalpostId=1234 har ugyldig status=UNDER_ARBEID og distribusjon av bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 avbrytes; bestillingsId=7882d37e-34f7-11e9-b210-d663bd873d93 og forsendelseId="));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
	}

	@Test
	public void shouldThrowPdlTechicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubFor(post("/pdl").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
						.value())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertNotNull(resultOnQdist008BackoutQueue);
			assertEquals(resultOnQdist008BackoutQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", ""));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalTechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_magleradresse_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_magleradresse_happypath.xml"));
		});
	}

	@Test
	public void shouldThrowBestemDokdistKanalMappingException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowOpprettForsendelseFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();
		stubFor(post(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL)
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
	}


	@Test
	public void shouldThrowOpprettForsendelseTechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();

		stubFor(post(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
	}

	@Test
	public void shouldThrowJournalpostAPITechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseFunctionalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();
		stubFor(put("/administrerforsendelse/oppdaterforsendelse")
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));
		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo((OPPDATERFORSENDELSE_URL))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseTechnicalException() throws Exception {
		stubGetDokumenttypeInfo();
		stubSTSToken();
		stubAzure();
		stubPostPdl();
		stubPostRdist001();
		stubFor(put(OPPDATERFORSENDELSE_URL)
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		stubFor(patch(urlMatching(String.format("/rest/journalpostapi/%s/oppdaterDistribusjonsinfo", 1234)))
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(qdist008Bq);
			assertThat(resultOnQdist008BackoutQueue).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo(STSSTRING)));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), patchRequestedFor(urlPathMatching("/rest/journalpostapi/1234/oppdaterDistribusjonsinfo"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/journalpostapi/oppdaterDistribusjonsinfoSHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo(DOKDISTADMIN_ADMINISTRERFORSENDELSE_URL))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo((OPPDATERFORSENDELSE_URL))));
	}

	private void stubPostRdist001() {
		stubFor(post("/rest/v1/administrerforsendelse")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
	}

	private void stubPostPdl() {
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
	}

	private void stubSTSToken() {
		stubFor(get(STSSTRING).willReturn(aResponse().withStatus(HttpStatus.OK
						.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
	}

	void stubAzure() {
		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response.json")));
	}

	private void stubGetDokumenttypeInfo() {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
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
			response = ((JAXBElement) response).getValue();
		}
		return (T) response;
	}

	protected TextMessage receiveTextMessage(final Queue queue) {
		return (TextMessage) jmsTemplate.receive(queue);
	}

	private String getRequestAsJson(String filename) throws IOException {

		File file = new ClassPathResource(filename).getFile();
		byte[] data = new byte[(int) file.length()];
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(data);
		fileInputStream.close();
		return new String(data);
	}
}



