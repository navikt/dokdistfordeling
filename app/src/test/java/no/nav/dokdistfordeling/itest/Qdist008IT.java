package no.nav.dokdistfordeling.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import no.nav.dokdistfordeling.Application;
import no.nav.dokdistfordeling.itest.config.ApplicationTestConfig;
import no.nav.dokdistfordeling.storage.Storage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, ApplicationTestConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist008IT {

	private static final String CALL_ID = "4321";
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
	private Storage storage;

	@BeforeEach
	public void setupBefore() {
		reset(storage);
		when(storage.get(any(String.class))).thenReturn(Optional.of(" "));
	}

	@Test
	public void shouldProcessForsendelse() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("brevogarkiv/arkiverDokumentProduksjonHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist009);
			assertThat(response.replaceAll("\r", "").replaceAll("\t", ""), is(classpathToString("qdist009/qdist009-happy.txt").replaceAll("\r", "").replaceAll("\t", "")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1")));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1")));
	}

	@Test
	public void shouldThrowForsendelseMapperException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"));
		});
	}

	@Test
	public void shouldThrowValidatonManglerHoveddokumentException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));
		});
	}

	@Test
	public void shouldThrowValidatonSamhandlerUtenAddresseException() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Disabled // todo add test for s3 throwing ValidationException in in forsendelseValidator
	@Test
	public void shouldThrowValidatonNotAvailableInS3Exception() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Test
	public void shouldThrowDokkatTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertThat(response, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
	}

	@Test
	public void shouldProcessForsendelseAktoerFunctionalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/hentIdentForAktoerIdFunctionalFail.xml")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));
	}

	@Test
	public void shouldThrowSettJournalpostAttributterTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);


		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertThat(response, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1")));
	}

	@Test
	public void shouldThrowOppdaterForsendelseFunctionalExceptionFunctionalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("brevogarkiv/arkiverDokumentProduksjonHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));
		});

		verify(putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	@Test
	public void shouldThrowOppdaterForsendelseFunctionalExceptionTechnicalException() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/administrerforsendelse/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("brevogarkiv/arkiverDokumentProduksjonHappy.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(backoutQueue);
			assertThat(response, is(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml")));
		});

		verify(putRequestedFor(urlEqualTo(("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST"))));
	}

	private void sendStringMessage(Queue queue, final String message, String callId) {
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

}



