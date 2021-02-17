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
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.config.cache.LokalCacheConfig.TKAT020_CACHE;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.storage.S3Configuration.BUCKET_NAME;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.amazonaws.services.s3.AmazonS3;
import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistfordeling.itest.config.Qdist008ItestConfig;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
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
import java.util.UUID;
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
	private Queue qdist010;

	@Inject
	private Queue qdist011;

	@Inject
	private Queue qdist013;

	@Inject
	private Queue backoutQueue;

	@Inject
	private AmazonS3 amazonS3;

	@Inject
	public CacheManager cacheManager;


	@BeforeEach
	public void setupBefore() {
		cacheManager.getCache(TKAT020_CACHE).clear();
		reset(amazonS3);
		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), anyString())).thenReturn(" ");

		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalPrint() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
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
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalDittNav() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalDittNav.json")));
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
			String response = receive(qdist010);
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("NAV_NO"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilDittNavOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalSDP() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalSDP.json")));
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
			String response = receive(qdist011);
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
			verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("SDP"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilSDPOutputHappy.json"))));
			verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
		});
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalTrygderetten() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalTrygderetten.json")));
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
			String response = receive(qdist013);
			assertThat(response.replaceAll("\r", "").replaceAll("\t", ""),
					is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
							.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()",
						equalTo(DistribusjonsKanalCode.TRYGDERETTEN.getJoarkUtsendingsKanal()))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilTrygderettenOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalLokalPrint() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalLokalPrint.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
			verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("L"))));
			verify(exactly(0), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilLokalPrintOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
		});
	}

	@Test
	public void shouldProcessForsendelseAndWithUtsendingskanalIngenDistribusjon() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalIngenDistribusjon.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
			verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
			verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-happy.json"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
			verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
					.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("INGEN_DISTRIBUSJON"))));
			verify(exactly(0), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
					.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseTilIngenDistribusjonOutputHappy.json"))));
			verify(exactly(0), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
		});
	}

	@Test
	public void shouldProcessForsendelseOnlyRequiredInputFieds() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
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


		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_only_required_fields_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(0), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/bestemkanal/bestemkanal-erArkivertFalse-happy.json"))));
		verify(exactly(0), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files/rjoark001/administrerForsendelseRequest_only_required_fields-happy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldProcessWithoutContactingDokkat() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
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
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(0), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithTittelAvoidDokkatHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldProcessForsendelseWithoutContactingPdl() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
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

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_avoid_pdl_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("out/out-happy.txt").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-organisasjon-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseWithOrganisasjonOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")));
	}

	@Test
	public void shouldPassOnCallId() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalTrygderetten.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("tjoark110/settJournalpostAttributterHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("rjoark001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		final String callId = UUID.randomUUID().toString();
		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), callId);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMsg = receiveTextMessage(qdist013);
			assertThat(responseTextMsg.getStringProperty(CALL_ID), is(callId));
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
	public void shouldThrowNotAvailableInS3ValidationException() throws Exception {

		when(amazonS3.getObjectAsString(eq(BUCKET_NAME), anyString())).thenReturn(null);
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

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
	}

	@Test
	public void shouldThrowPdlFunctionalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
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
		verify(exactly(1), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowPdlTechicalException() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
				.value())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertNotNull(resultOnQdist008BackoutQueue);
			assertEquals(resultOnQdist008BackoutQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(1), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/pdl")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalFunctionalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalTechnicalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowBestemDokdistKanalMappingException() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/bestemkanal-invalidCodeValue.json")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal")));
	}

	@Test
	public void shouldThrowPersisterForsendelseFunctionalException() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = receive(qdist008FunksjonellFeil);
			assertThat(resultOnQdist008FunksjonellFeilQueue.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
	}


	@Test
	public void shouldThrowPersisterForsendelseTechicalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
		stubFor(post("/bestemDistribusjonKanal").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("bestemkanal/distribusjonsKanalPrint.json")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008BackoutQueue = receive(backoutQueue);
			assertThat(resultOnQdist008BackoutQueue.replaceAll("\r", "")
					.replaceAll("\t", ""), is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml").replaceAll("\r", "")
					.replaceAll("\t", "")));
		});

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
	}

	@Test
	public void shouldThrowSettJournalpostAttributterTechnicalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
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

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/1111111")));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseFunctionalException() throws Exception {
		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
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

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseTechnicalException() throws Exception {

		stubFor(get("/dokkat-tkat020/" + DOKUMENTTYPE_ID).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse_happy.json")));
		stubFor(post("/pdl").willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("pdl/pdl-happy.json")));
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

		verify(exactly(1), getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(2), postRequestedFor(urlEqualTo("/pdl")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//endretAvNavn/text()", equalTo("qdist008"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/bestemDistribusjonKanal"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//bestemkanal/bestemkanal-happy.json"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//journalpostIdListe/text()", equalTo("1234"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1"))
				.withRequestBody(matchingXPath("//utsendingskanal/text()", equalTo("S"))));
		verify(exactly(1), postRequestedFor(urlEqualTo("/administrerforsendelse/v1"))
				.withRequestBody(equalToJson(getRequestAsJson("__files//rjoark001/administrerForsendelseTilPrintOutputHappy.json"))));
		verify(exactly(1), putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	private void sendStringMessage(Queue queue, final String message) {
		sendStringMessage(queue, message, null);
	}

	private void sendStringMessage(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
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
			response =  ((JAXBElement) response).getValue();
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



