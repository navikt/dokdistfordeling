package no.nav.dokdistfordeling.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.endpoints.Rdist002ValidationUtil;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import org.junit.jupiter.api.Test;

public class Rdist002ValidationUtilTest {

	private static final String BESTILLINGS_ID = "7cc280ce-4168-4204-8d03-8dbdc3c4fc32";

	private static final String JOURNALPOST_ID = "555555555";
	private static final String BATCH_ID = "66666";
	private static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
	private static final String ADRESSETYPE_NORSK = "norskPostadresse";
	private static final String ADRESSETYPE_UTENLANDSK = "utenlandskPostadresse";
	private static final String ADRESSELINJE1 = "eksempelveien 23 A";
	private static final String ADRESSELINJE2 = "eksempelveien 24 A";
	private static final String ADRESSELINJE3 = "eksempelveien 25 A";
	private static final String POSTSTED = "poststed";
	private static final String POSTNUMMER = "1337";
	private static final String LAND_NO = "NO";
	private static final String LAND_US = "US";
	private static final String DOKUMENTPRODAPP = "dokumentprodapp";
	private static final String DOK_TITTEL_1 = "DOK_TITTEL_1";
	private static final String DOK_TITTEL_2 = "DOK_TITTEL_2";
	private static final String BREVKODE = "000001";

	private static final String DOKUMENTTYPEID = "000001";
	private static final String TITTEL = "journalpostTittel";
	private static final String TEMA = "OPP";
	private static final String MOTTAKER_ID = "***gammelt_fnr***";
	private static final String MOTTAKER_NAVN = "Jan Neimansen";
	private static final String BRUKER_ID = "***gammelt_fnr***";
	private static final String BRUKER_NAVN = "***gammelt_fnr***";
	private static final String ORGNR = "776677665";
	private static final String ORG_NAVN = "eksempelcorp ASA";
	private static final String SAMHANDLER_KATOGORI = "HPR";
	private static final String SAMHANDLER_NAVN = "Betina Samhandlerson";
	private static final String SAMHANDLER_ID = "33322211";
	private static final String DOK_INFO_ID_1 = "666666666";
	private static final String DOK_INFO_ID_2 = "777777777";

	private Rdist002ValidationUtil rdist002ValidationUtil = new Rdist002ValidationUtil();

	// validate request

	@Test
	public void shouldValidateRequest() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		rdist002ValidationUtil.validateRequest(request);
	}

	@Test
	public void missingJournalpostIdShouldThrowValidationException() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		try {
			rdist002ValidationUtil.validateRequest(request);
		} catch (ValidationException e) {
			assertEquals(e.getMessage(), "Feltet journalpostId kan ikke være null eller tomt. Fikk journalpostId=null");
		}
	}

	@Test
	public void missingBestillendeFagsystemIdShouldThrowValidationException() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		try {
			rdist002ValidationUtil.validateRequest(request);
		} catch (ValidationException e) {
			assertEquals(e.getMessage(), "Feltet bestillendeFagsystem kan ikke være null eller tomt. Fikk bestillendeFagsystem=null");
		}
	}

	// validate Adresse

	private DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				POSTNUMMER,
				POSTSTED,
				ADRESSELINJE1,
				null,
				null,
				LAND_NO
		);
	}

	private DistribuerJournalpostRequestTo.AdresseTo createUtenlandskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_UTENLANDSK,
				null,
				null,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_US
		);
	}

	// validate journalpostAndDokumenter

}
