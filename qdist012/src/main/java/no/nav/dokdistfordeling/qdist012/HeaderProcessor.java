package no.nav.dokdistfordeling.qdist012;

import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.qdist012.Qdist012Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistfordeling.qdist012.Qdist012Route.PROPERTY_JOURNALPOST_ID;

import no.nav.dokdistfordeling.exception.functional.ForsendelseManglerPaakrevdHeaderFunctionalException;
import no.nav.dokdistfordeling.support.MDCHeaderProcessor;
import org.apache.camel.Exchange;

public class HeaderProcessor extends MDCHeaderProcessor {

	@Override
	public void process(Exchange exchange) {
		super.process(exchange);
		setBestillingsIdAsProperty(exchange);
		setJournalpostIdAsProperty(exchange);
	}

	private void setBestillingsIdAsProperty(Exchange exchange) {
		final String bestillingsId = exchange.getIn().getHeader(BESTILLINGS_ID, String.class);
		if (bestillingsId == null) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse uten påkrevd header bestillingsId");
		} else if (bestillingsId.trim().isEmpty()) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse med tom header bestillingsId");
		}
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);
	}

	private void setJournalpostIdAsProperty(Exchange exchange) {
		String journalpostId = exchange.getIn().getHeader(JOURNALPOST_ID, String.class);
		if (journalpostId == null) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse uten påkrevd header journalpostId");
		} else if (journalpostId.trim().isEmpty()) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse med tom header journalpostId");
		}
		exchange.setProperty(PROPERTY_JOURNALPOST_ID, journalpostId);
	}

}
