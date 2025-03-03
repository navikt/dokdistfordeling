package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.kodeverk.TvingKanal.TRYGDERETTEN;

public class TvingKanalValidator {

	public static final String TRYGDERETTEN_ORGNR = "974761084";

	private TvingKanalValidator() {
	}

	public static void validateTvingKanal(DistribuerJournalpost distribuerJournalpost, Journalpost journalpost) {

		var tvingKanal = distribuerJournalpost.tvingKanal();
		var avsenderMottaker = journalpost.getAvsenderMottaker();

		if (TRYGDERETTEN.equals(tvingKanal) && !TRYGDERETTEN_ORGNR.equals(avsenderMottaker.getId())) {
			throw new ValidationException(format("Ugyldig avsenderMottakerId for %s. avsenderMottakerId=%s men forventet avsenderMottakerId=%s", distribuerJournalpost.tvingKanal(), journalpost.getAvsenderMottaker(), TRYGDERETTEN_ORGNR));
		}
	}
}
