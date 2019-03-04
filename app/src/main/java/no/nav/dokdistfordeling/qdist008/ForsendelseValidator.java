package no.nav.dokdistfordeling.qdist008;

import static java.lang.String.format;

import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.exception.ValidationException;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.storage.Storage;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class ForsendelseValidator {

	private final Storage storage;

	public ForsendelseValidator(Storage storage) {
		this.storage = storage;
	}

	@Handler
	public void validate(DistribuerForsendelseTo distribuerForsendelseTo) {
		final DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo = distribuerForsendelseTo.getDistribusjonbestilling();
		assertThatForsendelseContainsExactlyOneHoveddokument(distribusjonbestillingTo);
		assertThatAdresseIsPresentIfMottakerIsSamhandler(distribusjonbestillingTo);
		assertThatBestillingsIdIsAValidUuid(distribusjonbestillingTo.getBestillingsId());
		assertThatDocumentsAreAvailableInS3(distribusjonbestillingTo);
	}

	private void assertThatForsendelseContainsExactlyOneHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		int numberOfHoveddokumenter = (int) distribusjonbestillingTo.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.HOVEDDOKUMENT))
				.count();

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
		try {
			UUID.fromString(bestillingsId);
		} catch (IllegalArgumentException exception) {
			throw new DokdistfordelingFunctionalException(format("bestillingsId er ikke en gyldig UUID (universally unique identifier). Fikk bestilling=%s", bestillingsId));
		}
	}

	private void assertThatDocumentsAreAvailableInS3(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		distribusjonbestillingTo.getDokumenter()
				.forEach(dokumentInformasjonTo -> {
					//Todo: Fjern put - kun for test!
					storage.put(dokumentInformasjonTo.getDokumentObjektReferanse(),"TESTPUT22");
					storage.get(dokumentInformasjonTo.getDokumentObjektReferanse())
							.orElseThrow(() -> new ValidationException(format("Kunne ikke finne dokument i S3 på key=dokumentObjektReferanse=%s", dokumentInformasjonTo
									.getDokumentObjektReferanse())));
				});
	}

}
