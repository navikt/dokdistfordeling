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
		validateForsendelseMetadata(distribuerJournalpostRequestTo);
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

	private static void validateForsendelseMetadata(DistribuerJournalpostRequestTo distribuerJournalpostRequest) {
		if (isOnlyForsendelseMetadataSet(distribuerJournalpostRequest) || isOnlyForsendelseMetadataTypeSet(distribuerJournalpostRequest)) {
			throw new ValidationException(format("forsendelsesMetadata og forsendelsesMetadataType må enten begge være satt, eller begge være null. Fikk forsendelsesmetadata=%s, forsendelsesmetadataType=%s",
					isBlank(distribuerJournalpostRequest.getForsendelseMetadata()) ? null : "****", distribuerJournalpostRequest.getForsendelseMetadataType()));
		}
	}

	private static boolean isOnlyForsendelseMetadataSet(DistribuerJournalpostRequestTo distribuerJournalpostRequest) {
		return distribuerJournalpostRequest.getForsendelseMetadata() != null && distribuerJournalpostRequest.getForsendelseMetadataType() == null;
	}

	private static boolean isOnlyForsendelseMetadataTypeSet(DistribuerJournalpostRequestTo distribuerJournalpostRequest) {
		return distribuerJournalpostRequest.getForsendelseMetadata() == null && distribuerJournalpostRequest.getForsendelseMetadataType() != null;
	}
}
