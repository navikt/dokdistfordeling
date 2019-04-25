package no.nav.dokdistfordeling.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import no.nav.dokdistfordeling.config.Rdist002TestConfig;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostResponseTo;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;

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
	private static final String ADRESSETYPE_UTENLANDS = "utenlandskPostadresse";
	private static final String ADDRESSELINJE1 = "eksempelveien 23 A";
	private static final String POSTSTED = "poststed";
	private static final String POSTNUMMER = "1337";
	private static final String LAND_NO = "NO";
	private static final String LAND_US = "US";
	private static final String DOKUMENTPRODAPP = "dokumentprodapp";
	private static final String DOKUMENTTYPEID = "000001";
	private static final String TITTEL = "dokkatTittel";

	@Inject
	protected TestRestTemplate restTemplate;

// happypath test should be the most complicated functioning path possible
	// journalpost distribusjonrequest, with all requestfields and token
	// receives a journalpost with both sladdet and arkiv from saf, as well as U, FERDIGSTILT, and

	@Test
	public void distribuerJournalpostHappyPath() {
		stubFor(post(urlMatching("/safgraphql")).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
				.withBodyFile("saf/safGraphQlResponse-happy.json"))); // todo remove request ?

		stubFor(get(urlMatching("/dokkat-tkat020/" + DOKUMENTTYPEID)).willReturn(aResponse().withStatus(HttpStatus.OK.value())
				.withHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				.withBodyFile("dokkat/tkat020-happy.json")));

		ResponseEntity<DistribuerJournalpostResponseTo> responseEntity = callDistribuerJournalpost(); // todo bruk rdist002request-happy.json ?
		DistribuerJournalpostResponseTo response = responseEntity.getBody();

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
	}


//		return Journalpost.builder() // todo saved for test
//				.journalposttype(Journalposttype.U)
//				.journalstatus(Journalstatus.FERDIGSTILT)
//				.bruker(new Bruker("54321", BrukerIdType.FNR))
//				.avsenderMottaker(AvsenderMottaker.builder().erLikBruker(true).build())
//				.dokumenter(Arrays.asList(
//						DokumentInfo.builder()
//								.tittel("The red wedding")
//								.brevkode("something")
//								.dokumentstatus(Dokumentstatus.FERDIGSTILT)
//								.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
//										.filnavn("Something else")
//										.saksbehandlerHarTilgang(true)
//										.variantformat(Variantformat.ARKIV).build()))
//								.build(),
//						DokumentInfo.builder()
//								.tittel("The blue wedding")
//								.brevkode("something other than else")
//								.dokumentstatus(Dokumentstatus.FERDIGSTILT)
//								.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
//										.filnavn("Something elseif")
//										.saksbehandlerHarTilgang(true)
//										.variantformat(Variantformat.ARKIV).build()))
//								.build()))
//				.build();

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
}
