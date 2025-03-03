package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;

import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullAndValidValueIgnoreCase;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmptyAndLengthNotGreaterThan;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNullOrValidValueIgnoreCase;


public class DistribuerJournalpostRequestValidator {

	private DistribuerJournalpostRequestValidator() {
	}

	public static void validateDistribuerJournalpostRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
		assertNotNullOrEmptyAndLengthNotGreaterThan("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem(), 20);
		assertNotNullOrEmptyAndLengthNotGreaterThan("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp(), 20);
		assertNotNullAndValidValueIgnoreCase("distribusjonstype", distribuerJournalpostRequestTo.getDistribusjonstype(), DistribusjonstypeCode.values());
		assertNotNullAndValidValueIgnoreCase("distribusjonstidspunkt", distribuerJournalpostRequestTo.getDistribusjonstidspunkt(), DistribusjonstidspunktCode.values());
		assertNullOrValidValueIgnoreCase("tvingKanal", distribuerJournalpostRequestTo.getTvingKanal(), TvingKanal.values());
	}
}
