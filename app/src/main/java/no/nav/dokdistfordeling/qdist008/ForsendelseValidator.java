package no.nav.dokdistfordeling.qdist008;

import static java.lang.String.format;

import no.nav.dokdistfordeling.exception.ValidationException;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class ForsendelseValidator {

	@Handler
	public void validate(DistribuerForsendelseTo distribuerForsendelseTo) {
		final DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo = distribuerForsendelseTo.getDistribusjonbestilling();
		assertThatForsendelseIsNotPreviouslyProcessed(distribusjonbestillingTo);
		assertThatForsendelseContainsExactlyOneHoveddokument(distribusjonbestillingTo);
		assertThatAdresseIsPresentIfMottakerIsSamhandler(distribusjonbestillingTo);
	}

	private void assertThatForsendelseIsNotPreviouslyProcessed(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo) {
		/**
		 * TODO Sjekk at det IKKE finnes innslag i database der DokumentInfo.DistribusjonsId = Distribusjonbestilling.bestillingsId OG DokumentInfo.DokumentStatus <> "OPPRETTET"
		 */
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
		if (distribusjonbestillingTo.getMottaker() instanceof DistribuerForsendelseTo.SamhandlerTo) {
			if (distribusjonbestillingTo.getAdresse() == null) {
				throw new ValidationException("Mottaker er av typen samhandler. Da er adresse et påkrevd felt i input til qdist008. Fant ingen adresse på bestilling");
			}
		}
	}

}
