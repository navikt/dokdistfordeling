package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE1;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSETYPE_NORSK;
import static no.nav.dokdistfordeling.UnitTestUtil.BATCH_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.UnitTestUtil.BRUKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.UnitTestUtil.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTNUMMER;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTSTED;
import static no.nav.dokdistfordeling.UnitTestUtil.createDokumentInfo1Builder;
import static no.nav.dokdistfordeling.UnitTestUtil.createDokumentInfo2Builder;
import static no.nav.dokdistfordeling.UnitTestUtil.createJournalpostBuilder;
import static no.nav.dokdistfordeling.UnitTestUtil.createNorskPostadresse;
import static no.nav.dokdistfordeling.UnitTestUtil.createPostadresseAdresstypeNull;
import static no.nav.dokdistfordeling.UnitTestUtil.createUtenlandskPostadresse;
import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VIKTIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Rdist002ValidationUtilTest {

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
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(VIKTIG.name())
				.build();
		rdist002ValidationUtil.validateRequest(request);
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingDistribusjonstidspunkt() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.journalpostId(JOURNALPOST_ID)
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstype(VIKTIG.name())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet distribusjonstidspunkt kan ikke være null eller tomt. Fikk distribusjonstidspunkt=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingDistribusjonstype() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.journalpostId(JOURNALPOST_ID)
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstidspunkt(KJERNETID.name())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet distribusjonstype kan ikke være null eller tomt. Fikk distribusjonstype=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingJournalpostId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet journalpostId kan ikke være null eller tomt. Fikk journalpostId=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingBestillendeFagsystemId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();

		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateRequest(request));
		assertEquals("Feltet bestillendeFagsystem kan ikke være null eller tomt. Fikk bestillendeFagsystem=null", thrownException.getMessage());
	}

	@Test
	public void shouldValidateNorskPostadresse() {
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);
		rdist002ValidationUtil.validateAdresse(createNorskPostadresse(), mottaker);
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
		rdist002ValidationUtil.validateAdresse(createUtenlandskPostadresse(), mottaker);
	}

	@Test
	public void shouldThrowValidationExceptionFromSamhandlerWithoutAdresse() {
		Samhandler mottaker = new Samhandler()
				.withNavn(MOTTAKER_NAVN)
				.withSamhandleridentifikator(MOTTAKER_ID);
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateAdresse(null, mottaker));
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
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateAdresse(adresseWithNullLand, mottaker));
		assertEquals("Feltet land kan ikke være null eller tomt. Fikk land=null", thrownException.getMessage());
	}


	@Test
	public void shouldThrowValidationExceptionForMissingAdresseType(){
		Person mottaker = new Person()
				.withNavn(MOTTAKER_NAVN)
				.withPersonidentifikator(MOTTAKER_ID);

		DistribuerJournalpostRequestTo.AdresseTo adresseWithNullAdressType = createPostadresseAdresstypeNull();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateAdresse(adresseWithNullAdressType, mottaker));
		assertEquals("Feltet adressetype kan ikke være null eller tomt. Fikk adressetype=null", thrownException.getMessage());

	}

	// validate journalpostAndDokumenter
	@Test
	public void shouldValidateJournalpostAndDokumenter() {
		Journalpost journalpost = createJournalpostBuilder().build();
		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);
	}

	@Test
	public void shouldValidateWithDokumentstatusNotEqualFerdigstilt() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						createDokumentInfo1Builder()
								.dokumentstatus("UNDER_REDIGERING")
								.build(),
						createDokumentInfo2Builder().build()))
				.build();
		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);
	}

	@Test
	public void shouldThrowValidationExceptionFromWrongJournalposttype() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalposttype(Journalposttype.I)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalposttype er ikke som forventet, fikk: I, men forventet U", thrownException.getMessage());
	}

	@Test
	public void shouldNotThrowValidationExceptionFromWrongJournalpoststatus() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalstatus(EKSPEDERT)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalpoststatus er ikke som forventet, fikk: EKSPEDERT, men forventet FERDIGSTILT", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(null)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker ikke være null eller tomt. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(null).type(BrukerIdType.FNR).build())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet brukerId ikke være null eller tomt. Fikk brukerId=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdTypeIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(BRUKER_ID).type(null).build())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.kodeverk.BrukerIdType ikke være null eller tomt. Fikk no.nav.dokdistfordeling.kodeverk.BrukerIdType=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMottakerIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.avsenderMottaker(null)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker ikke være null eller tomt. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMottakerNavnIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.avsenderMottaker(Journalpost.AvsenderMottaker.builder().navn(null).build())
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet mottakerNavn ikke være null eller tomt. Fikk mottakerNavn=null", thrownException.getMessage());
	}


	@Test
	public void shouldThrowValidationExceptionFromNoTittelInHoveddokument() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						createDokumentInfo1Builder()
								.tittel(null)
								.build(),
						createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For hoveddokumentet kan feltet tittel ikke være null eller tomt. Fikk tittel=null, dokumentInfoId=666666666", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromNoBrevkodeInHoveddokument() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						createDokumentInfo1Builder()
								.brevkode(null)
								.build(),
						createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("For hoveddokumentet kan feltet brevkode ikke være null eller tomt. Fikk brevkode=null, dokumentInfoId=666666666", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionNoDokumentvariantWithSaksbehandlerHarTilgangTrue() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(Arrays.asList(
						createDokumentInfo1Builder()
								.dokumentvarianter(Arrays.asList(

										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.variantformat(Variantformat.ARKIV).build(),
										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.variantformat(Variantformat.SLADDET).build()))
								.build(),
						createDokumentInfo2Builder().build()))
				.build();
		Exception thrownException = assertThrows(BrukerManglerTilgangTilDokumentFunctionalException.class, () -> rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost));
		assertEquals("Saksbehandler har ikke tilgang til noen av dokumentets variantformater. dokumentInfoId=666666666", thrownException.getMessage());
	}
}
