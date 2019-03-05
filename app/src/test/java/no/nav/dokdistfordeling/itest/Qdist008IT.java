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
import static java.util.stream.Collectors.joining;
import static org.awaitility.Awaitility.await;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import no.nav.dokdistfordeling.Application;
import no.nav.dokdistfordeling.itest.Config.ApplicationTestConfig;
import no.nav.dokdistfordeling.qdist008.DistribuerForsendelseTilSentralPrint;
import no.nav.dokdistfordeling.qdist008.ForsendelseValidator;
import no.nav.dokdistfordeling.storage.Storage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import wiremock.org.eclipse.jetty.util.annotation.Name;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@ExtendWith(SpringExtension.class)
@Import(ApplicationTestConfig.class)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist008IT {

	private static final String CALL_ID = "4321";
	private static final String DOKUMENTTYPE_ID = "1111111";
	private static final String FORSENDELSE_ID = "30818";

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	@Named("qdist008")
	private Queue qdist008;

	@Inject
	@Named("qdist009")
	private Queue qdist009;

	@Inject
	@Named("qdist008FunksjonellFeilQueue")
	private Queue qdist008FunksjonellFeil;

	@Inject
	@Named("qdist008BackoutQueue")
	private Queue backoutQueue;

	@Inject
	private ForsendelseValidator forsendelseValidator;

	@Inject
	private Storage storage;

	public Qdist008IT() throws JAXBException {
	}

	@BeforeEach
	public void setupBefore() {
		reset(storage);
		when(storage.get(any(String.class))).thenReturn(Optional.of(" "));
	}

	private JAXBContext jaxbContext = JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class);
	private Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

	@Test
	public void shouldProcessForsendelse() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
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
			DistribuerForsendelseTilSentralPrint resultingForsendelse = (DistribuerForsendelseTilSentralPrint) receive(qdist009);
			assertNotNull(resultingForsendelse);
			assertEquals(resultingForsendelse.getForsendelseId(), FORSENDELSE_ID);
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));
		verify(postRequestedFor(urlEqualTo("/arkiverdokumentproduksjon/v1")));
		verify(postRequestedFor(urlEqualTo("/administrerforsendelse/v1")));
	}

	@Test
	public void shouldProcessDistribuerForsendelseMapperFail() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mapperfail_bad_tema.xml"));
		});
	}

	@Test
	public void shouldProcessForsendelseValidatonFailManglerHoveddokument() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_mangler_hoveddokument.xml"));
		});
	}

	@Test
	public void shouldProcessForsendelseValidatonFailSamhandlerUtenAddresse() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Disabled // todo add test for s3 throwing ValidationException in in forsendelseValidator
	@Test
	public void shouldProcessForsendelseValidatonFailNotAvailableInS3() throws Exception {

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String resultOnQdist008FunksjonellFeilQueue = (String) receive(qdist008FunksjonellFeil);
			assertNotNull(resultOnQdist008FunksjonellFeilQueue);
			assertEquals(resultOnQdist008FunksjonellFeilQueue, classpathToString("qdist008/distribuerforsendelse_example_invalid_uuid.xml"));
		});
	}

	@Disabled
	@Test
	public void shouldProcessForsendelseDokkatFail() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBody("")));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Exception resultBackout = (Exception) receive(backoutQueue);
			assertNotNull(resultBackout);
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
	}

	@Test
	public void shouldProcessForsendelseAktoerFunctionalFail() throws Exception {

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

	@Disabled
	@Test
	public void shouldProcessForsendelseSettJournalpostAttributter() throws Exception {

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPE_ID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokumentinfov4/tkat020-happy.json")));
		stubFor(post("/aktoerv2")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("aktoerv2/aktoerV2HentIdentForAktoerHappy.xml")));
		stubFor(post("/arkiverdokumentproduksjon/v1")
				.willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.withBodyFile("brevogarkiv/arkiverDokumentProduksjonTechicalFail.xml")));
		stubFor(post("/administrerforsendelse/v1").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("rdist001/administrerForsendelseV1Happy.json")));
		stubFor(put("/administrerforsendelse/v1?forsendelseId=" + FORSENDELSE_ID + "&forsendelseStatus=KLAR_FOR_DIST")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())));

		sendStringMessage(qdist008, classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"), CALL_ID);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			DistribuerForsendelseTilSentralPrint resultingForsendelse = (DistribuerForsendelseTilSentralPrint) receive(backoutQueue);
			assertNotNull(resultingForsendelse);
			assertEquals(resultingForsendelse.getForsendelseId(), FORSENDELSE_ID);
		});

		verify(getRequestedFor(urlEqualTo("/dokkat-tkat020/" + DOKUMENTTYPE_ID)));
		verify(postRequestedFor(urlEqualTo("/aktoerv2")));

	}

	// todo: when included
	// Dokdiststatusupdater Technical fail, //settJournalpostAttributter
	// Dokdiststatusupdater Functional fail,

	private void sendStringMessage(Queue queue, final String message, String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			return msg;
		});
	}

	private String classpathToString(String classpathResource) throws IOException {
		InputStream inputStream = new ClassPathResource(classpathResource).getInputStream();
		return new BufferedReader(new InputStreamReader(inputStream))
				.lines()
				.collect(joining("\n"));
	}

	protected Object receive(Queue queue) throws JAXBException {
		Object response = jmsTemplate.receiveAndConvert(queue);
		try {
			return unmarshaller.unmarshal(new StringReader(response.toString()));
		} catch (Exception e) {
			return response.toString();
		}
	}

}



