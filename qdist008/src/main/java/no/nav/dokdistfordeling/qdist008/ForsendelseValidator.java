package no.nav.dokdistfordeling.qdist008;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.functional.BestillingsIdInvalidUuidFunctionalException;
import no.nav.dokdistfordeling.exception.functional.OjectNotFoundInBucketFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.storage.BucketStorage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.util.Qdist008Util.countHoveddokument;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Slf4j
@Component
public class ForsendelseValidator {

	private final BucketStorage storage;
	private static final String BDOK001_PREFIX = "BDOK100";

	public ForsendelseValidator(BucketStorage storage) {
		this.storage = storage;
	}

	@Handler
	public void validate(DistribuerForsendelseTo distribuerForsendelseTo) {
		final DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo = distribuerForsendelseTo.getDistribusjonbestilling();
		assertThatForsendelseContainsExactlyOneHoveddokument(distribusjonbestillingTo);
		assertThatAdresseIsPresentIfMottakerIsSamhandler(distribusjonbestillingTo);
		assertThatBestillingsIdIsAValidUuid(distribusjonbestillingTo.getBestillingsId());

		assertThatDocumentsAreAvailableInBucket(distribusjonbestillingTo);
	}

	private void assertThatForsendelseContainsExactlyOneHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		int numberOfHoveddokumenter = countHoveddokument(distribusjonbestillingTo);

		if (numberOfHoveddokumenter != 1) {
			throw new ValidationException(format("Forsendelsen må inneholde nøyaktig ett hoveddokument. Fant %s hoveddokument(er) på forsendelsen", numberOfHoveddokumenter));
		}
	}

	private void assertThatAdresseIsPresentIfMottakerIsSamhandler(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		if (distribusjonbestillingTo.getMottaker().isSamhandler() && distribusjonbestillingTo.getAdresse() == null) {
			throw new ValidationException("Mottaker er av typen samhandler. Da er adresse et påkrevd felt i input til qdist008. Fant ingen adresse på bestilling");
		}
	}

	private void assertThatBestillingsIdIsAValidUuid(String bestillingsId) {
		if (!bestillingsId.startsWith(BDOK001_PREFIX)) {
			try {
				UUID.fromString(bestillingsId);
			} catch (IllegalArgumentException exception) {
				throw new BestillingsIdInvalidUuidFunctionalException(format("bestillingsId er ikke en gyldig UUID (universally unique identifier). Fikk bestilling=%s", bestillingsId));
			}
		}
	}

	private void assertThatDocumentsAreAvailableInBucket(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		distribusjonbestillingTo.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					String dokumentObjektReferanse = dokumentInformasjonTo.getDokumentObjektReferanse();
					if (!storage.exists(dokumentObjektReferanse)) {
						throw new OjectNotFoundInBucketFunctionalException(format("Fant ikke objectName i Google Cloud Storage. objectName=%s", dokumentObjektReferanse));
					}
				});
	}
}
