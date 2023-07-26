package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateAdresse;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateDistribuerJournalpostRequest;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateJournalpostAndDokumenter;
import static no.nav.dokdistfordeling.UnitTestUtil.BATCH_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.UnitTestUtil.BRUKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.UnitTestUtil.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.createDokumentInfo1Builder;
import static no.nav.dokdistfordeling.UnitTestUtil.createDokumentInfo2Builder;
import static no.nav.dokdistfordeling.UnitTestUtil.createJournalpostBuilder;
import static no.nav.dokdistfordeling.UnitTestUtil.createMottaker;
import static no.nav.dokdistfordeling.UnitTestUtil.createNorskPostadresse;
import static no.nav.dokdistfordeling.UnitTestUtil.createPostadresseAdresstypeNull;
import static no.nav.dokdistfordeling.UnitTestUtil.createUtenlandskPostadresse;
import static no.nav.dokdistfordeling.UnitTestUtil.createUtenlandskPostadresseWithAdresselinje1;
import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VIKTIG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Rdist002ValidationUtilTest {

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
		validateDistribuerJournalpostRequest(request);
	}

	@ParameterizedTest
	@CsvSource({
			"null,hadde en ugyldig verdi. Fikk distribusjonstidspunkt=null. Gyldige verdier er ",
			" ,kan ikke være null eller tomt. Fikk distribusjonstidspunkt=null",
			"InVaLiD,hadde en ugyldig verdi. Fikk distribusjonstidspunkt=InVaLiD. Gyldige verdier er "})
	public void shouldThrowValidationExceptionForVariousDistribusjonstidspunkt(String distribusjonstidspunkt, String errorMessage) {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.journalpostId(JOURNALPOST_ID)
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstype(VIKTIG.name())
				.distribusjonstidspunkt(distribusjonstidspunkt)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateDistribuerJournalpostRequest(request));
		assertThat(thrownException.getMessage(), containsString("Feltet distribusjonstidspunkt " + errorMessage));
	}

	@ParameterizedTest
	@CsvSource({
			"null,hadde en ugyldig verdi. Fikk distribusjonstype=null. Gyldige verdier er ",
			" ,kan ikke være null eller tomt. Fikk distribusjonstype=null",
			"InVaLiD,hadde en ugyldig verdi. Fikk distribusjonstype=InVaLiD. Gyldige verdier er "})
	public void shouldThrowValidationExceptionFromMissingDistribusjonstype(String distribusjonstype, String errorMessage) {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.journalpostId(JOURNALPOST_ID)
				.dokumentProdApp(DOKUMENTPRODAPP)
				.distribusjonstidspunkt(KJERNETID.name())
				.distribusjonstype(distribusjonstype)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateDistribuerJournalpostRequest(request));
		assertThat(thrownException.getMessage(), containsString("Feltet distribusjonstype " + errorMessage));
	}

	@Test
	public void shouldThrowValidationExceptionFromMissingJournalpostId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateDistribuerJournalpostRequest(request));
		assertEquals("Feltet journalpostId kan ikke være null eller tomt. Fikk journalpostId=null", thrownException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			"ikke_et_postnummer,Feltet postnummer må være et gyldig tall med 4 siffer. Fikk postnummer=ikke_et_postnummer",
			"12345,Feltet postnummer må være et gyldig tall med 4 siffer. Fikk postnummer=12345",
			",Feltet postnummer må være et gyldig tall med 4 siffer. Fikk postnummer=null"
	})
	public void ShouldThrowValidationExceptionWhenBadNorskPostnummer(String postnummer, String expectedMessage) {
		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(UnitTestUtil.createNorskPostadresseWithPostnummer(postnummer), createMottaker()));
		assertEquals(expectedMessage, thrownException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			",Feltet poststed kan ikke være null eller tomt. Fikk poststed=null",
			"'', Feltet poststed kan ikke være null eller tomt. Fikk poststed= "
	})
	public void shouldThrowValdationExceptionWhenNoPostnummerOrPoststedForNorskPostadresse(String poststed, String expectedMessage) {
		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(UnitTestUtil.createNorskPostadresseWithPostSted(poststed), createMottaker()));
		assertEquals(expectedMessage, thrownException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			",Feltet adresselinje1 kan ikke være null eller tomt. Fikk adresselinje1=null",
			"'',Feltet adresselinje1 kan ikke være null eller tomt. Fikk adresselinje1= "
	})
	public void shouldThrowValidationExceptionForMissingAdresselinje1WhenUtenlandskAdresse(String adresselinje1, String expectedMessage) {
		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(createUtenlandskPostadresseWithAdresselinje1(adresselinje1), createMottaker()));
		assertEquals(expectedMessage, thrownException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({
			"NOR,Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=NOR",
			"'',Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=",
			",Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=null"
	})
	public void shouldThrowValidationExceptionForBadLandkode(String landkode, String expectedMessage) {
		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(UnitTestUtil.createPostadresseWithLandkode(landkode), createMottaker()));
		assertEquals(expectedMessage, thrownException.getMessage());
	}

	@ParameterizedTest
	@CsvSource({"BV", "EE", "KG", "NO", "SE", "DK"})
	public void shouldValidateGoodLandkode(String landkode) {
		validateAdresse(UnitTestUtil.createPostadresseWithLandkode(landkode), createMottaker());
	}


	@Test
	public void shouldThrowValidationExceptionFromMissingBestillendeFagsystemId() {
		DistribuerJournalpostRequestTo request = DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP)
				.build();

		Exception thrownException = assertThrows(ValidationException.class, () -> validateDistribuerJournalpostRequest(request));
		assertEquals("Feltet bestillendeFagsystem kan ikke være null eller tomt. Fikk bestillendeFagsystem=null", thrownException.getMessage());
	}

	@Test
	public void shouldValidateNorskPostadresse() {
		validateAdresse(createNorskPostadresse(), createMottaker());
	}

	@Test
	public void shouldValidateAdresseWithoutAdresse() {
		validateAdresse(null, createMottaker());
	}

	@Test
	public void shouldValidateAdresseWithUtenlandskPostadresse() {
		validateAdresse(createUtenlandskPostadresse(), createMottaker());
	}

	@Test
	public void shouldThrowValidationExceptionFromSamhandlerWithoutAdresse() {
		Samhandler samhandler = new Samhandler();
		samhandler.setNavn(MOTTAKER_NAVN);
		samhandler.setSamhandleridentifikator(MOTTAKER_ID);

		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(null, samhandler));
		assertEquals("For mottaker av type samhandler kan ikke adresse være null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionForMissingAdresseType() {
		Person person = new Person();
		person.setNavn(MOTTAKER_NAVN);
		person.setPersonidentifikator(MOTTAKER_ID);

		DistribuerJournalpostRequestTo.AdresseTo adresseWithNullAdressType = createPostadresseAdresstypeNull();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateAdresse(adresseWithNullAdressType, person));
		assertEquals("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType= null", thrownException.getMessage());

	}

	// validate journalpostAndDokumenter
	@Test
	public void shouldValidateJournalpostAndDokumenter() {
		Journalpost journalpost = createJournalpostBuilder().build();
		validateJournalpostAndDokumenter(journalpost);
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
		validateJournalpostAndDokumenter(journalpost);
	}

	@Test
	public void shouldThrowValidationExceptionFromWrongJournalposttype() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalposttype(Journalposttype.I)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalposttype er ikke som forventet, fikk: I, men forventet U", thrownException.getMessage());
	}

	@Test
	public void shouldNotThrowValidationExceptionFromWrongJournalpoststatus() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalstatus(EKSPEDERT)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("journalpoststatus er ikke som forventet, fikk: EKSPEDERT, men forventet FERDIGSTILT", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(null)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker ikke være null eller tomt. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.Bruker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(null).type(BrukerIdType.FNR).build())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet brukerId ikke være null eller tomt. Fikk brukerId=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromBrukerIdTypeIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.bruker(Journalpost.Bruker.builder().id(BRUKER_ID).type(null).build())
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.kodeverk.BrukerIdType ikke være null eller tomt. Fikk no.nav.dokdistfordeling.kodeverk.BrukerIdType=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMottakerIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.avsenderMottaker(null)
				.build();
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("For journalposter kan feltet no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker ikke være null eller tomt. Fikk no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost.AvsenderMottaker=null", thrownException.getMessage());
	}

	@Test
	public void shouldThrowValidationExceptionFromMottakerNavnIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.avsenderMottaker(Journalpost.AvsenderMottaker.builder().navn(null).build())
				.build();
		Exception thrownException = Assertions.assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
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
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
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
		Exception thrownException = assertThrows(ValidationException.class, () -> validateJournalpostAndDokumenter(journalpost));
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
		Exception thrownException = assertThrows(BrukerManglerTilgangTilDokumentFunctionalException.class, () -> validateJournalpostAndDokumenter(journalpost));
		assertEquals("Systembruker eller saksbehandler har ikke tilgang til dokumentInfoId=666666666 og kan derfor ikke bestille distribusjon. " +
					 "For saksbehandlere betyr dette ofte at saksbehandleren mangler tilgang til tema eller brukers enhet i AXSYS. " +
					 "For systembrukere betyr dette ofte at systembrukeren ikke ligger inn med riktig tema-role i Azure IAC-konfigurasjonen for SAF sin <env-config.json>. " +
					 "Kontakt oss på #team_dokumentløsninger for bistand.", thrownException.getMessage());
	}
}
