package no.nav.dokdistfordeling.unittest;

import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ADRESSELINJE1;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ADRESSETYPE_NORSK;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BATCH_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BRUKER_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.MOTTAKER_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.POSTNUMMER;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.POSTSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.Rdist002ValidationUtil;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class Rdist002ValidationUtilTest {

	private UnitTestUtil unitTestUtil = new UnitTestUtil();
	private Rdist002ValidationUtil rdist002ValidationUtil = new Rdist002ValidationUtil();

	// validate request
	@Test
	public void shouldValidateRequest() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(unitTestUtil.createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		rdist002ValidationUtil.validateRequest(request);
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingJournalpostId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(unitTestUtil.createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet journalpostId kan ikke være null eller tomt. Fikk journalpostId=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingBestillendeFagsystemId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.adresse(unitTestUtil.createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();

		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet bestillendeFagsystem kan ikke være null eller tomt. Fikk bestillendeFagsystem=null", thrownException.getMessage());
	}

	@Test
	public void shouldValidateNorskPostadresse() {
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
		rdist002ValidationUtil.validateAdresse(unitTestUtil.createNorskPostadresse(), mottaker);
	}

	@Test
	public void shouldValidateAdresseWithoutAdresse() {
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
		rdist002ValidationUtil.validateAdresse(null, mottaker);
	}

	@Test
	public void shouldValidateAdresseWithUtenlandskPostadresse() {
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
		rdist002ValidationUtil.validateAdresse(unitTestUtil.createUtenlandskPostadresse(), mottaker);
	}

	@Test
	public void shouldThrowValidationExceptionFromSamhandlerWithoutAdresse() {
		Samhandler mottaker = new Samhandler()
				.withNavn(MOTTAKER_NAVN)
				.withSamhandleridentifikator(MOTTAKER_ID);
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateAdresse(null, mottaker));
		assertEquals("For mottaker av type samhandler kan ikke adresse være null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingLand() {
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
		DistribuerJournalpostRequestTo.AdresseTo adresseWithNullLand = new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				POSTNUMMER,
				POSTSTED,
				ADRESSELINJE1,
				null,
				null,
				null
		);
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateAdresse(adresseWithNullLand, mottaker));
		assertEquals("Feltet land kan ikke være null eller tomt. Fikk land=null", thrownException.getMessage());
	}

	// validate journalpostAndDokumenter
	@Test
	public void shouldValidateJournalpostAndDokumenter() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder().build();
		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);
	}

	@Test
	public void shouldThrowValidationExceptionFromWrongJournalposttype() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.journalposttype(Journalposttype.I)
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalposttype er ikke som forventet, fikk: I, men forventet U", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromWrongJournalpoststatus() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.journalstatus(EKSPEDERT)
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalpoststatus er ikke som forventet, fikk: EKSPEDERT, men forventet FERDIGSTILT", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIsNull() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.bruker(null)
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker kan ikke være null. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdIsNull() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(null).type(BrukerIdType.FNR).build())
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("Feltet brukerId kan ikke være null eller tomt. Fikk brukerId=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdTypeIsNull() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(BRUKER_ID).type(null).build())
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("no.nav.dokdistfordeling.kodeverk.BrukerIdType kan ikke være null. Fikk no.nav.dokdistfordeling.kodeverk.BrukerIdType=null", thrownException.getMessage());
	}


	@Test
	public void shouldThrowValidationExceptionFromMottakerIsNull() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.avsenderMottaker(null)
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker kan ikke være null. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromNoTittelInHoveddokument() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						unitTestUtil.createDokumentInfo1Builder()
								.tittel(null)
								.build(),
						unitTestUtil.createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("Feltet tittel kan ikke være null eller tomt. Fikk tittel=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromNoBrevkodeInHoveddokument() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						unitTestUtil.createDokumentInfo1Builder()
								.brevkode(null)
								.build(),
						unitTestUtil.createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("Feltet brevkode kan ikke være null eller tomt. Fikk brevkode=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromNullDokumentstatus() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						unitTestUtil.createDokumentInfo1Builder()
								.dokumentstatus(null)
								.build(),
						unitTestUtil.createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("dokumentstatus er ikke som forventet, fikk: null, men forventet FERDIGSTILT", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromWrongDokumentstatus() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						unitTestUtil.createDokumentInfo1Builder()
								.dokumentstatus("UNDER_REDIGERING")
								.build(),
						unitTestUtil.createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("dokumentstatus er ikke som forventet, fikk: UNDER_REDIGERING, men forventet FERDIGSTILT", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionNoDokumentvariantWithSaksbehandlerHarTilgangTrue() {
		Journalpost journalpost = unitTestUtil.createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						unitTestUtil.createDokumentInfo1Builder()
								.dokumentvarianter(Arrays.asList(

										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.variantformat(Variantformat.ARKIV).build(),
										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.variantformat(Variantformat.SLADDET).build()))
								.build(),
						unitTestUtil.createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = Assertions.assertThrows(BrukerManglerTilgangTilDokumentFunctionalException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("ingen variantformater av dokumentet med tilgang for saksbehandler ble funnet.", thrownException.getMessage());
	}
}
