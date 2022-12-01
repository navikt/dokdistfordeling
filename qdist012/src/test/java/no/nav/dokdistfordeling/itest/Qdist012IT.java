package no.nav.dokdistfordeling.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.exception.technical.FailedBucketUploadTechnicalException;
import no.nav.dokdistfordeling.itest.config.Qdist012TestConfig;
import no.nav.dokdistfordeling.storage.BucketStorage;
import no.nav.dokdistfordeling.storage.DokdistDokument;
import no.nav.dokdistfordeling.storage.JsonSerializer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.RetryConstants.MAX_ATTEMPTS_SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@EnableAutoConfiguration
@SpringBootTest(classes = {Qdist012TestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist012IT {

	private static byte[] TEST_FILE_BYTES1 = "TestThis1".getBytes();
	private static byte[] TEST_FILE_BYTES2 = "TestThis2".getBytes();
	private static byte[] TEST_FILE_BYTES3 = "TestThis3".getBytes();
	private static String BESTILLINGS_ID = "4a7d638a-6a63-11e9-a923-1681be663d3e";
	private static final String BESTILLINGS_ID_ATTRIBUTE = "bestillingsId";
	private static String JOURNALPOST_ID = "arkivId";
	private static String JOURNALPOST_ID_ATTRIBUTE = "journalpostId";

	@Autowired
	private JmsTemplate jmsTemplate;

	@Autowired
	private Queue qdist008;

	@Autowired
	private Queue qdist012FunksjonellFeil;

	@Autowired
	private Queue qdist012;

	@Autowired
	private Queue backoutQueue;

	@Autowired
	private BucketStorage bucketStorage;

	@Value("${hentdokumenter_fra_joark_crypto_password}")
	private String encryptionPassphrase;

	@BeforeEach
	public void setupBefore() {
		Mockito.reset(bucketStorage);
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void happyPath() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(TEST_FILE_BYTES1)));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(TEST_FILE_BYTES2)));

		ArgumentCaptor<String> argCaptorDokdistDokument = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> argCaptorDokumentObjektReferanse = ArgumentCaptor.forClass(String.class);

		final String callId = UUID.randomUUID().toString();
		encryptAndSendStringMessageWithHeaders(qdist012, classpathToString("qdist012/qdist012-happy.xml"), callId);

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist008);
			assertEquals(callId, responseTextMessage.getStringProperty(CALL_ID));
			String response = responseTextMessage.getText();
			assertNotNull(response);

			//Alle felter bortsett fra objektreferanse verifiseres her
			String cleanedResponse = replaceUuidBetween(response, "dokumentObjektReferanse", "<dokumentObjektReferanse>", "</dokumentObjektReferanse>");
			assertThat(cleanedResponse).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypath.xml"));

			//verifiser at riktige objekt lagres til bucket
			DistribuerForsendelse unmarshaledResponse = unmarshalDistribuerForsendelseFromXmlString(response);
			Mockito.verify(bucketStorage, times(2))
					.upload(argCaptorDokumentObjektReferanse.capture(), argCaptorDokdistDokument.capture(), eq(BESTILLINGS_ID));

			DokdistDokument dokdistDokument1 = JsonSerializer.deserialize(argCaptorDokdistDokument.getAllValues()
					.get(0), DokdistDokument.class);
			DokdistDokument dokdistDokument2 = JsonSerializer.deserialize(argCaptorDokdistDokument.getAllValues()
					.get(1), DokdistDokument.class);
			assertEquals(new String(TEST_FILE_BYTES1), new String(dokdistDokument1.getPdf()));
			assertEquals(new String(TEST_FILE_BYTES2), new String(dokdistDokument2.getPdf()));

			//objektreferanse verifiseres her
			assertEquals(argCaptorDokumentObjektReferanse.getAllValues().get(0), unmarshaledResponse.getDistribusjonbestilling()
					.getDokumenter()
					.get(0)
					.getDokumentObjektReferanse());
			assertEquals(argCaptorDokumentObjektReferanse.getAllValues().get(1), unmarshaledResponse.getDistribusjonbestilling()
					.getDokumenter()
					.get(1)
					.getDokumentObjektReferanse());
		});
	}

	@Test
	public void happyPathWithMissingVedlegg() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happyMedVedlegg.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(TEST_FILE_BYTES1)));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(TEST_FILE_BYTES2)));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg2/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(TEST_FILE_BYTES3)));

		ArgumentCaptor<String> argCaptorDokdistDokument = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> argCaptorDokumentObjektReferanse = ArgumentCaptor.forClass(String.class);

		final String callId = UUID.randomUUID().toString();
		encryptAndSendStringMessageWithHeaders(qdist012, classpathToString("qdist012/qdist012-happyUtenVedlegg.xml"), callId);

		await().atMost(100, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist008);
			assertEquals(callId, responseTextMessage.getStringProperty(CALL_ID));
			String response = responseTextMessage.getText();
			assertNotNull(response);

			//Alle felter bortsett fra objektreferanse verifiseres her
			String cleanedResponse = replaceUuidBetween(response, "dokumentObjektReferanse", "<dokumentObjektReferanse>", "</dokumentObjektReferanse>");
			assertThat(cleanedResponse).isEqualToIgnoringWhitespace(classpathToString("qdist008/distribuerforsendelse_example_happypathMedVedlegg.xml"));

			//verifiser at riktige objekt lagres til bucket
			DistribuerForsendelse unmarshaledResponse = unmarshalDistribuerForsendelseFromXmlString(response);
			Mockito.verify(bucketStorage, times(3))
					.upload(argCaptorDokumentObjektReferanse.capture(), argCaptorDokdistDokument.capture(), eq(BESTILLINGS_ID));

			DokdistDokument dokdistDokument1 = JsonSerializer.deserialize(argCaptorDokdistDokument.getAllValues()
					.get(0), DokdistDokument.class);
			DokdistDokument dokdistDokument2 = JsonSerializer.deserialize(argCaptorDokdistDokument.getAllValues()
					.get(1), DokdistDokument.class);
			DokdistDokument dokdistDokument3 = JsonSerializer.deserialize(argCaptorDokdistDokument.getAllValues()
					.get(2), DokdistDokument.class);
			assertEquals(new String(TEST_FILE_BYTES1), new String(dokdistDokument1.getPdf()));
			assertEquals(new String(TEST_FILE_BYTES2), new String(dokdistDokument2.getPdf()));
			assertEquals(new String(TEST_FILE_BYTES3), new String(dokdistDokument3.getPdf()));

			//objektreferanse verifiseres her
			assertEquals(argCaptorDokumentObjektReferanse.getAllValues().get(0), unmarshaledResponse.getDistribusjonbestilling()
					.getDokumenter()
					.get(0)
					.getDokumentObjektReferanse());
			assertEquals(argCaptorDokumentObjektReferanse.getAllValues().get(1), unmarshaledResponse.getDistribusjonbestilling()
					.getDokumenter()
					.get(1)
					.getDokumentObjektReferanse());
			assertEquals(argCaptorDokumentObjektReferanse.getAllValues().get(2), unmarshaledResponse.getDistribusjonbestilling()
					.getDokumenter()
					.get(2)
					.getDokumentObjektReferanse());
		});
	}

	@Test
	public void shouldThrowFunctionalExceptionMissingBestillingsId() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, JOURNALPOST_ID);
			return msg;
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist012FunksjonellFeil);
			assertNotNull(responseTextMessage);
			assertEquals(message, decryptXml(responseTextMessage.getText()));
			assertEquals(JOURNALPOST_ID, responseTextMessage.getStringProperty(JOURNALPOST_ID_ATTRIBUTE));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowFunctionalExceptionEmptyBestillingsId() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, "");
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, JOURNALPOST_ID);
			return msg;
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist012FunksjonellFeil);
			assertNotNull(responseTextMessage);
			assertEquals(message, decryptXml(responseTextMessage.getText()));
			assertEquals("", responseTextMessage.getStringProperty(BESTILLINGS_ID_ATTRIBUTE));
			assertEquals(JOURNALPOST_ID, responseTextMessage.getStringProperty(JOURNALPOST_ID_ATTRIBUTE));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowFunctionalExceptionMissingJournalpostIdHeader() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, BESTILLINGS_ID);
			return msg;
		});
		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist012FunksjonellFeil);
			assertNotNull(responseTextMessage);
			assertEquals(message, decryptXml(responseTextMessage.getText()));
			assertEquals(BESTILLINGS_ID, responseTextMessage.getStringProperty(BESTILLINGS_ID_ATTRIBUTE));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowFunctionalExceptionEmptyJournalpostIdheader() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, BESTILLINGS_ID);
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, "");
			return msg;
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			TextMessage responseTextMessage = receiveTextMessage(qdist012FunksjonellFeil);
			assertNotNull(responseTextMessage);
			assertEquals(message, decryptXml(responseTextMessage.getText()));
			assertEquals(BESTILLINGS_ID, responseTextMessage.getStringProperty(BESTILLINGS_ID_ATTRIBUTE));
			assertEquals("", responseTextMessage.getStringProperty(JOURNALPOST_ID_ATTRIBUTE));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowFunctionalCryptoExceptionWrongSalt() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, "thisKeyShouldBeBestillingsId").encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, BESTILLINGS_ID);
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, JOURNALPOST_ID);
			return msg;
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(qdist012FunksjonellFeil);
			assertNotNull(response);
			assertEquals(message, new Crypto(encryptionPassphrase, "thisKeyShouldBeBestillingsId").decrypt(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowFunctionalCryptoExceptionMessageNotEncrypted() throws Exception {
		String message = classpathToString("qdist012/qdist012-happy.xml");
		jmsTemplate.send(qdist012, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			msg.setText(message);
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, BESTILLINGS_ID);
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, JOURNALPOST_ID);
			return msg;
		});

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(qdist012FunksjonellFeil);
			assertNotNull(response);
			assertEquals(message, response);
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), eq(BESTILLINGS_ID));
		verify(exactly(0), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowBucketTechnicalException() throws Exception {
		doThrow(new FailedBucketUploadTechnicalException("Feilet ved persistering av dokument til S3")).when(bucketStorage)
				.upload(any(), any(), eq(BESTILLINGS_ID));
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(Base64.getEncoder().encode(TEST_FILE_BYTES1))));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(backoutQueue);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(1)).upload(any(), any(), any());
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(1), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowStsTechnicalException() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR
				.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(backoutQueue);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), any());
		verify(exactly(MAX_ATTEMPTS_SHORT), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(0), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowSafTechnicalException() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(backoutQueue);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), any());
		verify(exactly(1), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(MAX_ATTEMPTS_SHORT), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowSafFunctionalException() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(qdist012FunksjonellFeil);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), any());
		verify(exactly(1), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowHentDokumentTechnicalException() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(backoutQueue);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), any());
		verify(exactly(MAX_ATTEMPTS_SHORT + 1), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(MAX_ATTEMPTS_SHORT), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	@Test
	public void shouldThrowHentDokumentFunctionalException() throws Exception {
		stubFor(get("/stsRest/token?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(post("/safGraphQL").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

		String message = classpathToString("qdist012/qdist012-happy.xml");
		encryptAndSendStringMessageWithHeaders(qdist012, message);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receiveFromBoqAndAssertHeaders(qdist012FunksjonellFeil);
			assertNotNull(response);
			assertEquals(message, decryptXml(response));
		});
		Mockito.verify(bucketStorage, times(0)).upload(any(), any(), any());
		verify(exactly(2), getRequestedFor(urlEqualTo("/stsRest/token?grant_type=client_credentials&scope=openid")));
		verify(exactly(1), postRequestedFor(urlEqualTo("/safGraphQL")));
		verify(exactly(1), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV")));
		verify(exactly(0), getRequestedFor(urlEqualTo("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET")));
	}

	private void encryptAndSendStringMessageWithHeaders(Queue queue, final String message) {
		encryptAndSendStringMessageWithHeaders(queue, message, null);
	}

	private void encryptAndSendStringMessageWithHeaders(Queue queue, final String message, final String callId) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			if (callId != null) {
				msg.setStringProperty(CALL_ID, callId);
			}
			msg.setStringProperty(BESTILLINGS_ID_ATTRIBUTE, BESTILLINGS_ID);
			msg.setStringProperty(JOURNALPOST_ID_ATTRIBUTE, JOURNALPOST_ID);
			return msg;
		});
	}

	private String classpathToString(String classpathResource) throws IOException {
		try (InputStream inputStream = new ClassPathResource(classpathResource).getInputStream()) {
			return IOUtils.toString(inputStream, UTF_8);
		}
	}

	protected TextMessage receiveTextMessage(final Queue queue) {
		return (TextMessage) jmsTemplate.receive(queue);
	}

	protected String receive(final Queue queue) throws JMSException {
		return ((TextMessage) jmsTemplate.receive(queue)).getText();
	}

	@SuppressWarnings("unchecked")
	private String receiveFromBoqAndAssertHeaders(Queue queue) throws JMSException {
		TextMessage textMessage = (TextMessage) jmsTemplate.receive(queue);
		assertEquals(BESTILLINGS_ID, textMessage.getStringProperty(BESTILLINGS_ID_ATTRIBUTE));
		assertEquals(JOURNALPOST_ID, textMessage.getStringProperty(JOURNALPOST_ID_ATTRIBUTE));
		return textMessage.getText();
	}

	private String replaceUuidBetween(String theString, String value, String open, String close) {
		return theString.replaceAll("(" + open + ")[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}(" + close + ")", "$1" + value + "$2");
	}

	private DistribuerForsendelse unmarshalDistribuerForsendelseFromXmlString(String xmlString) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(DistribuerForsendelse.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			return (DistribuerForsendelse) unmarshaller.unmarshal(new StringReader(xmlString));
		} catch (JAXBException | IllegalArgumentException e) {
			throw new IllegalArgumentException("Kunne ikke marshalle bestilling til xmlString");
		}
	}

	private String decryptXml(String xml) {
		return new Crypto(encryptionPassphrase, BESTILLINGS_ID).decrypt(xml);
	}
}



