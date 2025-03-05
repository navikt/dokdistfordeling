package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullAndValidValueIgnoreCase;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmptyAndLengthNotGreaterThan;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNullOrValidValueIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;


public class DistribuerJournalpostRequestValidator {

	private DistribuerJournalpostRequestValidator() {
	}

	public static void validateDistribuerJournalpostRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		validateJournalpostId(distribuerJournalpostRequestTo.getJournalpostId());

		assertNotNullOrEmptyAndLengthNotGreaterThan("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem(), 20);
		assertNotNullOrEmptyAndLengthNotGreaterThan("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp(), 20);
		assertNotNullAndValidValueIgnoreCase("distribusjonstype", distribuerJournalpostRequestTo.getDistribusjonstype(), DistribusjonstypeCode.values());
		assertNotNullAndValidValueIgnoreCase("distribusjonstidspunkt", distribuerJournalpostRequestTo.getDistribusjonstidspunkt(), DistribusjonstidspunktCode.values());
		assertNullOrValidValueIgnoreCase("tvingKanal", distribuerJournalpostRequestTo.getTvingKanal(), TvingKanal.values());
	}

	private static void validateJournalpostId(String journalpostId) {
		if (isBlank(journalpostId) || !isNumeric(journalpostId)) {
			throw new ValidationException(format("Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=%s", journalpostId));
		}

		try {
			Long.parseLong(journalpostId);
		} catch (NumberFormatException e) {
			throw new ValidationException(format("Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=%s", journalpostId));
		}
	}
}
