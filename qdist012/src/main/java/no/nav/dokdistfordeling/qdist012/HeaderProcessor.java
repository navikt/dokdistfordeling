package no.nav.dokdistfordeling.qdist012;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.qdist012.Qdist012Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistfordeling.qdist012.Qdist012Route.PROPERTY_JOURNALPOST_ID;

import no.nav.dokdistfordeling.exception.functional.ForsendelseManglerPaakrevdHeaderFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public class HeaderProcessor implements Processor {

	private static final String JOURNALPOST_ID = "journalpostId";

	@Override
	public void process(Exchange exchange) {
		setBestillingsIdAsPropertyAndAddCallIdToMdc(exchange);
		setJournalpostIdAsProperty(exchange);
	}

	private void setBestillingsIdAsPropertyAndAddCallIdToMdc(Exchange exchange) {
		final String callId = exchange.getIn().getHeader(CALL_ID, String.class);
		if (callId == null) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse uten påkrevd header callId");
		} else if (callId.trim().isEmpty()) {
			throw new ForsendelseManglerPaakrevdHeaderFunctionalException("qdist012 har mottatt forsendelse med tom header callId");
		}
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, callId);
		MDC.put(CALL_ID, exchange.getProperty(PROPERTY_BESTILLINGS_ID, String.class));
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
