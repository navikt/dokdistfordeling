package no.nav.dokdistfordeling.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static no.nav.dokdistfordeling.constants.MdcConstants.CALL_ID;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import no.nav.dokdistfordeling.config.Rdist002TestConfig;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostResponseTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@SpringBootTest(classes = {Rdist002TestConfig.class},
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
public class Rdist002IT {

	private static final String TEMP_OIDC_TOKEN = "eyAidHlwIjogIkpXVCIsICJraWQiOiAiMWwySmtDb1RMMTBibWVBeHlsZzR4Umk4ajJZPSIsICJhbGciOiAiUlMyNTYiIH0.eyAiYXRfaGFzaCI6ICJ4RklSS0dpTWZ4ZFVPS3c0ZmQ4MW9BIiwgInN1YiI6ICJaOTkyMzEwIiwgImF1ZGl0VHJhY2tpbmdJZCI6ICJiZDdlYWE0ZC1mYzIzLTQ2ZGMtOGRjZi1iMjJmNzU1NDExZjQtMjAyMDc5MzQiLCAiaXNzIjogImh0dHBzOi8vaXNzby1xLmFkZW8ubm86NDQzL2lzc28vb2F1dGgyIiwgInRva2VuTmFtZSI6ICJpZF90b2tlbiIsICJhdWQiOiAiaWRhLXEiLCAiY19oYXNoIjogInctbGx3ZlJMenVpRFBselpkY1BhenciLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZWN0Lm9wcyI6ICIyZmNlNWU1ZS02ODdjLTQ5ZmYtOTRjYS1jNzE2OGVmY2M2MmQiLCAiYXpwIjogImlkYS1xIiwgImF1dGhfdGltZSI6IDE1NTUwNzQ3NjcsICJyZWFsbSI6ICIvIiwgImV4cCI6IDE1NTUwNzgzNjcsICJ0b2tlblR5cGUiOiAiSldUVG9rZW4iLCAiaWF0IjogMTU1NTA3NDc2NyB9.orrUotLp8SMkCpigVhkAUlw9Rx5tigBrYNVv3j8fTmkIe-I1MEI0xctxM-tnLbrgcW3I-3Ye_bkS4KplhR4spnG9hT45L1dD-yoLsu8R6cD1PklMsx8m93XmaTHDReGZAI3uKO4KSPcQHyVE7-tIc6CWYqbVXWmEUxUsHNYm3bWO_0rZ-Su6CWVCEBz3yWa85rUcPn0Il-_BWkgF-0YhOWJn3ndKAl_96ARmR-nllhUnQDYqHk2DwYLWnz_WOb4HuuqxKRP5i1h8zHwGIR6VORCzWgFViiFNTPT54Mtr2fZtVinP8W70JoRZ1pKbk-bYK4ErJgACU8npdGBZYTZa6g";
	private static final String DISTRIBUER_JOURNALPOST_URI = "/rest/v1/distribuerjournalpost";
	private static final String JOURNALPOSTID = "555555555";
	private static final String BATCHID = "66666";
	private static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
	private static final String ADRESSETYPE_NORSK = "norskPostadresse";
	private static final String ADRESSETYPE_UTENLANDSK = "utenlandskPostadresse";
	private static final String ADDRESSELINJE1 = "eksempelveien 23 A";
	private static final String ADDRESSELINJE2 = "eksempelveien 24 A";
	private static final String ADDRESSELINJE3 = "eksempelveien 25 A";
	private static final String POSTSTED = "poststed";
	private static final String POSTNUMMER = "1337";
	private static final String LAND_NO = "NO";
	private static final String LAND_US = "US";
	private static final String DOKUMENTPRODAPP = "dokumentprodapp";
	private static final String DOKUMENTTYPEID = "000001";
	private static final String TITTEL = "journalpostTittel";

	private static final String ARKIVID = "555555555";
	private static final String TEMA = "OPP";
	private static final String MOTTAKER_ID = "***gammelt_fnr***";
	private static final String MOTTAKER_NAVN = "Jan Neimansen";
	private static final String BRUKER_ID = "***gammelt_fnr***";
	private static final String DOKUMENT_INFO_ID = "555555555";

	private @Value("${hentdokumenter_fra_joark_crypto_password}")
	String encryptionPassphrase;

	@Inject
	private JmsTemplate jmsTemplate;

	@Inject
	private Queue qdist012;

	@Inject
	protected TestRestTemplate restTemplate;

	@Test
	public void distribuerJournalpostHappyPath() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json")));

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = callDistribuerJournalpost();
		DistribuerJournalpostResponseTo restResponse = responseEntity.getBody();

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(36, restResponse.getBestillingsId().length());

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			Message qdist012ResultMessage = jmsTemplate.receive(qdist012);
			HentDokumenterFraJoark qdist012Result = unmarshalHentDokumenterFraJoarkFromXmlStringAndDecrypt(qdist012ResultMessage);

			assertNotNull(qdist012Result);
			assertQdist012Result(qdist012Result.getDistribusjonbestilling(), restResponse.getBestillingsId());
		});
	}

	//itest paths:
	// happy
	// mangler authorizationHeader
	// feil i requestvalidering
	// unauthorized mot saf
	// tom respons fra saf
	// teknisk feil i saf
	// feil i validering av journalpost fra saf
	// Ugyldig dokumentTypeId ifra dokkat
	// teknisk feil i dokkat
	// Feil ved legging på kø mot qdist012

	private void assertQdist012Result(Distribusjonbestilling qdist012Result, String restResponseBestillingsId) {
		assertNotNull(qdist012Result);
		assertEquals(restResponseBestillingsId, qdist012Result.getBestillingsId());

		assertEquals(BATCHID, qdist012Result.getBatchId());
		assertEquals(BESTILLENDEFAGSYSTEM, qdist012Result.getBestillendeFagsystem());
		assertEquals(TEMA, qdist012Result.getTema());
		assertEquals(TITTEL, qdist012Result.getForsendelseTittel());
		assertArkivInformasjon(qdist012Result.getArkivInformasjon());
		assertMottaker((Person) qdist012Result.getMottaker());
		assertBruker((Person) qdist012Result.getBruker());
		assertNorskPostadresse((NorskPostadresse) qdist012Result.getAdresse());
		assertEquals(DOKUMENTPRODAPP, qdist012Result.getDokumentProdApp());
		assertDokumenter(qdist012Result.getDokumenter());
	}

	private void assertArkivInformasjon(ArkivInformasjon arkivInformasjon) {
		assertEquals(ARKIVID, arkivInformasjon.getArkivId());
		assertEquals(TEMA, arkivInformasjon.getArkivSystem());
	}

	private void assertMottaker(Person person) {
		assertEquals(MOTTAKER_ID, person.getPersonidentifikator());
		assertEquals(MOTTAKER_NAVN, person.getNavn());
	}

	private void assertBruker(Person person) {
		assertEquals(BRUKER_ID, person.getPersonidentifikator());
		assertNull(person.getNavn());
	}

	private void assertNorskPostadresse(NorskPostadresse adresse) {
		assertNotNull(adresse);
		assertEquals(LAND_NO, adresse.getLand());
		assertEquals(POSTNUMMER, adresse.getPostnummer());
		assertEquals(POSTSTED, adresse.getPoststed());
		assertEquals(ADDRESSELINJE1, adresse.getAdresselinje1());
		assertNull(adresse.getAdresselinje2());
		assertNull(adresse.getAdresselinje3());
	}

	private void assertUtenlandskPostadresse(UtenlandskPostadresse adresse) {
		assertNotNull(adresse);
		assertEquals(LAND_NO, adresse.getLand());
		assertEquals(ADDRESSELINJE1, adresse.getAdresselinje1());
		assertEquals(ADDRESSELINJE2, adresse.getAdresselinje2());
		assertEquals(ADDRESSELINJE3, adresse.getAdresselinje3());
	}

	private void assertDokumenter(List<DokumentInformasjon> dokumenter) {
		assertThat(dokumenter.size(), greaterThan(0));

		dokumenter.forEach(dokumentInformasjon -> {
			if (HOVEDDOKUMENT.name().equals(dokumentInformasjon.getTilknyttetSom())) {
				assertEquals(dokumentInformasjon.getRekkefolge(), 1);
			} else {
				assertThat(dokumentInformasjon.getRekkefolge(), greaterThan(1));
				assertEquals(VEDLEGG.name(), dokumentInformasjon.getTilknyttetSom());
			}
			assertSladdetDokument(dokumentInformasjon);
		});
	}

	private void assertSladdetDokument(DokumentInformasjon dokument) {
		assertEquals(DOKUMENTTYPEID, dokument.getDokumenttypeId());
		assertEquals(DOKUMENT_INFO_ID, dokument.getArkivDokumentInfoId());
		assertEquals(SLADDET.name(), dokument.getVariantFormat());
	}

	private ResponseEntity<DistribuerJournalpostResponseTo> callDistribuerJournalpost() {
		return this.restTemplate.exchange(DISTRIBUER_JOURNALPOST_URI, HttpMethod.POST, createHttpEntity(), DistribuerJournalpostResponseTo.class);
	}

	private HttpEntity createHttpEntity() {
		return new HttpEntity<>(defaultDistribuerJournalpostRequestTo(), createHeaders());
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + TEMP_OIDC_TOKEN);
		return headers;
	}

	private DistribuerJournalpostRequestTo defaultDistribuerJournalpostRequestTo() {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOSTID)
				.batchId(BATCHID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(new DistribuerJournalpostRequestTo.AdresseTo(
						ADRESSETYPE_NORSK,
						POSTNUMMER,
						POSTSTED,
						ADDRESSELINJE1,
						null,
						null,
						LAND_NO
				))
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();

	}

	private HentDokumenterFraJoark unmarshalHentDokumenterFraJoarkFromXmlStringAndDecrypt(Message message) throws JMSException, JAXBException {
		String bestillingsId = message.getStringProperty(CALL_ID);
		String encryptedAndMarshaledBody = ((TextMessage) message).getText();

		String decryptedAndMarshaledBody = new Crypto(encryptionPassphrase, bestillingsId).decrypt(encryptedAndMarshaledBody);

		StringReader sr = new StringReader(decryptedAndMarshaledBody);
		JAXBContext jaxbContext = JAXBContext.newInstance(HentDokumenterFraJoark.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		return (HentDokumenterFraJoark) unmarshaller.unmarshal(sr);
	}
}
