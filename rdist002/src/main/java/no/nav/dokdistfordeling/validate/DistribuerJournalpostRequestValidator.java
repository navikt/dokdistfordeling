package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
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
		assertNullOrValidValueIgnoreCase("forsendelsesMetadataType", distribuerJournalpostRequestTo.getForsendelseMetadataType(), ForsendelseMetadataType.values());
		validateForsendelseMetadata(distribuerJournalpostRequestTo);
	}

	private static void validateJournalpostId(String journalpostId) {
		if (isBlank(journalpostId) || !isNumeric(journalpostId)) {
			throw new ValidationException(format("Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=%s", journalpostId));
		}

		try {
			Long.parseLong(journalpostId);
		} catch (NumberFormatException _) {
			throw new ValidationException(format("Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=%s", journalpostId));
		}
	}

	private static void validateForsendelseMetadata(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		if (isOnlyForsendelseMetadataSet(distribuerJournalpostRequestTo) || isOnlyForsendelseMetadataTypeSet(distribuerJournalpostRequestTo)) {
			throw new ValidationException(format("forsendelsesMetadata og forsendelsesMetadataType må enten begge være satt, eller begge være null. Fikk forsendelsesmetadata=%s, forsendelsesmetadataType=%s",
					isBlank(distribuerJournalpostRequestTo.getForsendelseMetadata()) ? null : "****", distribuerJournalpostRequestTo.getForsendelseMetadataType()));
		}
	}

	private static boolean isOnlyForsendelseMetadataSet(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		return distribuerJournalpostRequestTo.getForsendelseMetadata() != null && distribuerJournalpostRequestTo.getForsendelseMetadataType() == null;
	}

	private static boolean isOnlyForsendelseMetadataTypeSet(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		return distribuerJournalpostRequestTo.getForsendelseMetadata() == null && distribuerJournalpostRequestTo.getForsendelseMetadataType() != null;
	}
}
