package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.InvalidFiltypeException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static no.nav.dokdistfordeling.TestData.BRUKER_ID;
import static no.nav.dokdistfordeling.TestData.DOK_INFO_ID_1;
import static no.nav.dokdistfordeling.TestData.createDokumentInfo1Builder;
import static no.nav.dokdistfordeling.TestData.createDokumentInfo2Builder;
import static no.nav.dokdistfordeling.TestData.createJournalpostBuilder;
import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.kodeverk.Journalposttype.U;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.PDF;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.PDFA;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.validateJournalpostAndDokumenter;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

class JournalpostValidatorTest {

	@Test
	void shouldValidateJournalpostAndDokumenter() {
		Journalpost journalpost = createJournalpostBuilder().build();

		assertDoesNotThrow(() -> validateJournalpostAndDokumenter(journalpost));
	}

	@Test
	void shouldValidateWithDokumentstatusNotEqualFerdigstilt() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(List.of(
						createDokumentInfo1Builder()
								.dokumentstatus("UNDER_REDIGERING")
								.build(),
						createDokumentInfo2Builder().build()))
				.build();
		validateJournalpostAndDokumenter(journalpost);
	}

	@Test
	void shouldThrowValidationExceptionWhenJournalposttypeIsNull() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalposttype(null)
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("For journalposter kan feltet Journalposttype ikke være null eller tomt. Fikk Journalposttype=null");
	}

	@ParameterizedTest
	@EnumSource(value = Journalposttype.class, mode = EXCLUDE, names = "U")
	void shouldThrowValidationExceptionWhenJournalposttypeIsNotUtgaande(Journalposttype journalposttype) {
		Journalpost journalpost = createJournalpostBuilder()
				.journalposttype(journalposttype)
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("Journalpostfeltet journalposttype er ikke som forventet, fikk: %s, men forventet %s", journalposttype, U.name());

	}

	@Test
	void shouldThrowValidationExceptionWhenJournalstatustypeIsNotFerdigstilt() {
		Journalpost journalpost = createJournalpostBuilder()
				.journalstatus(EKSPEDERT)
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("Journalpostfeltet journalpoststatus er ikke som forventet, fikk: %s, men forventet %s", EKSPEDERT, FERDIGSTILT);
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenRequiredFieldIsNull(Journalpost journalpost, String field) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("For journalposter kan feltet %s ikke være null eller tomt. Fikk %s=null", field, field);
	}

	static Stream<Arguments> shouldThrowValidationExceptionWhenRequiredFieldIsNull() {
		return Stream.of(
				Arguments.of(createJournalpostBuilder().bruker(null).build(), "Bruker"),
				Arguments.of(createJournalpostBuilder().bruker(Journalpost.Bruker.builder().id(BRUKER_ID).type(null).build()).build(), "BrukerIdType"),
				Arguments.of(createJournalpostBuilder().avsenderMottaker(null).build(), "AvsenderMottaker")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenRequiredFieldIsEmpty(Journalpost journalpost, String field) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("For journalposter kan feltet %s ikke være null eller tomt. Fikk %s= ", field, field);
	}

	static Stream<Arguments> shouldThrowValidationExceptionWhenRequiredFieldIsEmpty() {
		return Stream.of(
				Arguments.of(createJournalpostBuilder().bruker(Journalpost.Bruker.builder().id(" ").type(BrukerIdType.FNR).build()).build(), "brukerId"),
				Arguments.of(createJournalpostBuilder().avsenderMottaker(Journalpost.AvsenderMottaker.builder().navn(" ").build()).build(), "mottakerNavn")
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {" "})
	@NullSource
	void shouldThrowValidationExceptionWhenNoTittelInHoveddokument(String tittel) {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(List.of(
						createDokumentInfo1Builder()
								.tittel(tittel)
								.build(),
						createDokumentInfo2Builder()
								.build()))
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("For hoveddokumentet kan feltet tittel ikke være null eller tomt. Fikk tittel=%s, dokumentInfoId=%s", tittel, DOK_INFO_ID_1);
	}

	@ParameterizedTest
	@ValueSource(strings = {" "})
	@NullSource
	void shouldThrowValidationExceptionFromNoBrevkodeInHoveddokument(String brevkode) {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(List.of(
						createDokumentInfo1Builder()
								.brevkode(brevkode)
								.build(),
						createDokumentInfo2Builder()
								.build()))
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("For hoveddokumentet kan feltet brevkode ikke være null eller tomt. Fikk brevkode=%s, dokumentInfoId=%s", brevkode, DOK_INFO_ID_1);
	}

	@Test
	void shouldThrowValidationExceptionWhenNoDokumentvariantWithSaksbehandlerHarTilgang() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(List.of(
						createDokumentInfo1Builder()
								.dokumentvarianter(List.of(
										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.filtype(PDF)
												.variantformat(ARKIV).build(),
										Journalpost.Dokumentvariant.builder()
												.saksbehandlerHarTilgang(false)
												.filtype(PDFA)
												.variantformat(Variantformat.SLADDET).build()))
								.build(),
						createDokumentInfo2Builder().build()))
				.build();

		assertThatExceptionOfType(BrukerManglerTilgangTilDokumentFunctionalException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("Systembruker eller saksbehandler har ikke tilgang til dokumentInfoId=%s og kan derfor ikke bestille distribusjon. " +
							 "For saksbehandlere betyr dette ofte at saksbehandleren mangler tilgang til tema eller brukers enhet i AXSYS. " +
							 "For systembrukere betyr dette ofte at systembrukeren ikke ligger inn med riktig tema-role i Azure IAC-konfigurasjonen for SAF sin <env-config.json>. " +
							 "Kontakt oss på #team_dokumentløsninger for bistand.", DOK_INFO_ID_1);

	}

	@Test
	void shouldThrowValidationExceptionWhenNoDokumentvariantWithFiltypePdfOrPdfa() {
		Journalpost journalpost = createJournalpostBuilder()
				.dokumenter(List.of(
						createDokumentInfo1Builder()
								.dokumentvarianter(List.of(
										Journalpost.Dokumentvariant.builder()
												.filtype("JSON")
												.saksbehandlerHarTilgang(true)
												.variantformat(ARKIV).build()))
								.build(),
						createDokumentInfo2Builder().build()))
				.build();

		assertThatExceptionOfType(InvalidFiltypeException.class)
				.isThrownBy(() -> validateJournalpostAndDokumenter(journalpost))
				.withMessage("Ugyldig dokumentvariant=%s eller filtype=JSON, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres", ARKIV.name());
	}

}