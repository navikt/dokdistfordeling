package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static no.nav.dokdistfordeling.TestData.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.TestData.MOTTAKER_ID;
import static no.nav.dokdistfordeling.TestData.createAvsenderMottakerBuilder;
import static no.nav.dokdistfordeling.TestData.createJournalpostBuilder;
import static no.nav.dokdistfordeling.kodeverk.TvingKanal.TRYGDERETTEN;
import static no.nav.dokdistfordeling.validate.TvingKanalValidator.TRYGDERETTEN_ORGNR;
import static no.nav.dokdistfordeling.validate.TvingKanalValidator.validateTvingKanal;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TvingKanalValidatorTest {

	@ParameterizedTest
	@EnumSource(TvingKanal.class)
	void shouldValidateTvingKanal(TvingKanal tvingKanal) {
		var avsenderMottakerId = tvingKanal.equals(TRYGDERETTEN) ? TRYGDERETTEN_ORGNR : MOTTAKER_ID;

		DistribuerJournalpost distribuerJournalpost = DistribuerJournalpost.builder()
				.tvingKanal(tvingKanal)
				.build();

		Journalpost journalpost = createJournalpostBuilder()
				.avsenderMottaker(createAvsenderMottakerBuilder()
						.id(avsenderMottakerId)
						.build())
				.build();

		validateTvingKanal(distribuerJournalpost, journalpost);
	}

	@Test
	void shouldValidateEmptyTvingKanal() {
		DistribuerJournalpost distribuerJournalpost = DistribuerJournalpost.builder()
				.journalpostId(JOURNALPOST_ID)
				.tvingKanal(null)
				.build();

		Journalpost journalpost = createJournalpostBuilder().build();

		validateTvingKanal(distribuerJournalpost, journalpost);
	}

	@Test
	void shouldThrowValidationExceptionWhenTvingkanalIsTrygderettenAndAvsenderMottakerIsNotTrygderetten() {
		DistribuerJournalpost distribuerJournalpost = DistribuerJournalpost.builder()
				.journalpostId(JOURNALPOST_ID)
				.tvingKanal(TRYGDERETTEN)
				.build();
		Journalpost journalpost = createJournalpostBuilder().build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateTvingKanal(distribuerJournalpost, journalpost))
				.withMessage("Ugyldig avsenderMottakerId for %s. avsenderMottakerId=Journalpost.AvsenderMottaker(id=09876543210, navn=Jan Neimansen, type=null) men forventet avsenderMottakerId=%s",
						TRYGDERETTEN,
						TRYGDERETTEN_ORGNR);
	}

}