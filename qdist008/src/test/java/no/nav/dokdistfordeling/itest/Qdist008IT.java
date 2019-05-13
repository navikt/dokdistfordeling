package no.nav.dokdistfordeling.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistfordeling.itest.config.Qdist008ItestConfig;
import no.nav.dokdistfordeling.storageaws.AwsStorage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {Qdist008ItestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist008IT {

	private static final String FORSENDELSE_ID = "33333";
	private static final String DOKUMENTTYPE_ID = "1111111";

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist008;

	@Inject
	private Queue qdist008FunksjonellFeil;

	@Inject
	private Queue qdist009;

	@Inject
	private Queue backoutQueue;

	@Inject
	private AwsStorage awsStorage;

	@Inject
	public CacheManager cacheManager;


	@BeforeEach
	public void setupBefore() {
		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(awsStorage);
		when(awsStorage.get(any(String.class))).thenReturn(Optional.of(" "));

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void shouldProcessForsendelse() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist009/qdist009-happy.txt").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
	}

	@Test
	public void shouldProcessWithoutContactingDokkat() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_har_tittel_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist009/qdist009-happy.txt").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithTittelAvoidDokkatHappy.json"))));
	}

	@Test
	public void shouldProcessForsendelseWithoutContactingAktoerV2() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_aktoerv2_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist009/qdist009-happy.txt").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(0), postRequestedFor(urlEqualTo("/aktoerv2")));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-organisasjon-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithOrganisasjonOutputHappy.json"))));
	}

	@Test
	public void shouldThrowForsendelseMapperException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"));
		});
	}

	@Test
	public void shouldThrowValidatonManglerHoveddokumentException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));
		});
	}

	@Test
	public void shouldThrowValidatonSamhandlerUtenAddresseException() throws Exception {

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
	public void shouldThrowValidatonNotAvailableInS3Exception() throws Exception {

		when(awsStorage.get(any(String.class))).thenReturn(null);
		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist008BackoutQueue);
			assertEquals(resultOnQdist008BackoutQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});
	}

	@Test
	public void shouldThrowDokkatTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
	}

	@Test
	public void shouldThrowAktoerV2FunctionalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/hentIdentForAktoerIdFunctionalFail.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));
	}

	@Test
	public void shouldThrowAktoerV2TechicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/hentIdentForAktoerIdTechnicalFail.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist008BackoutQueue);
			assertEquals(resultOnQdist008BackoutQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalFunctionalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalMappingException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/bestemkanal-invalidCodeValue.json")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowPersisterForsendelseFunctionalException() throws Exception {
		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
	}


	@Test
	public void shouldThrowPersisterForsendelseTechicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
	}

	@Test
	public void shouldThrowSettJournalpostAttributterTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.withBody("tjoark110/settJournalpostAttributterHappy.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseFunctionalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
		verify(putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), postRequestedFor(urlEqualTo("/aktoerv2"))
				.withRequestBody(matchingXPath("//aktoerId/text()", equalTo("***gammelt_fnr***01"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseOutputHappy.json"))));
		verify(putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	private void sendStringMessage(Queue queue, final String message) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			return msg;
		});
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		String message = IOUtils.toString(inputStream, UTF_8);
		IOUtils.closeQuietly(inputStream);
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

	private String getRequestAsJson(String filename) throws IOException {

		File file = new ClassPathResource(filename).getFile();
		byte[] data = new byte[(int) file.length()];
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(data);
		fileInputStream.close();
		return new String(data);
	}
}



