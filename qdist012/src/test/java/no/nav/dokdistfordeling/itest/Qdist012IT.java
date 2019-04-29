package no.nav.dokdistfordeling.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.itest.config.Qdist012TestConfig;
import no.nav.dokdistfordeling.storage.Storage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {Qdist012TestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Qdist012IT {

	private static byte[] TEST_FILE_BYTES1 = "TestThis1".getBytes();
	private static byte[] TEST_FILE_BYTES2 = "TestThis2".getBytes();
	private static String BESTILLINGS_ID = "4a7d638a-6a63-11e9-a923-1681be663d3e";

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist008;

	@Inject
	private Queue qdist012FunksjonellFeil;

	@Inject
	private Queue qdist012;

	@Inject
	private Queue backoutQueue;

	@Inject
	private Storage storage;

	@Value("${hentdokumenter_fra_joark_crypto_password}")
	private String encryptionPassphrase;

	@BeforeEach
	public void setupBefore() {
		WireMock.reset();
		WireMock.resetAllRequests();
		WireMock.removeAllMappings();
	}

	@Test
	public void happyPath() throws Exception {
		stubFor(get("/stsRest?grant_type=client_credentials&scope=openid").willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("sts/stsResponse-happy.json")));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdHoveddok/ARKIV").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(Base64.getEncoder().encode(TEST_FILE_BYTES1))));
		stubFor(get("/hentdokument/arkivId/arkivDokumentInfoIdVedlegg/SLADDET").willReturn(
				aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(org.springframework.http.HttpHeaders.CONTENT_TYPE, APPLICATION_PDF_VALUE)
						.withBody(Base64.getEncoder().encode(TEST_FILE_BYTES2))));

		encryptAndSendStringMessage(qdist012, classpathToString("qdist012/qdist012-happy.xml"));

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			String response = receive(qdist008);
			assertThat(response, is(notNullValue()));
		});


	}


	private void encryptAndSendStringMessage(Queue queue, final String message) {
		jmsTemplate.send(queue, session -> {
			TextMessage msg = new ActiveMQTextMessage();
			final String encryptedMessage = new Crypto(encryptionPassphrase, BESTILLINGS_ID).encrypt(message);
			msg.setText(encryptedMessage);
			msg.setStringProperty("callId", BESTILLINGS_ID);
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



