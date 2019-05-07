package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_BESTILLINGS_ID;

import no.nav.dokdistfordeling.exception.functional.ForsendelseManglerBestillingsIdFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.xml.XPathBuilder;
import org.slf4j.MDC;

/**
 * @author Erik Bråten, Visma Consulting.
 */
public class IdsProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		setBestillingsIdAsPropertyAndAddCallIdToMdc(exchange);
	}

	private void setBestillingsIdAsPropertyAndAddCallIdToMdc(Exchange exchange) {
		String bestillingsId = XPathBuilder.xpath("//bestillingsId/text()").evaluate(exchange, String.class);
		if (bestillingsId == null) {
			throw new ForsendelseManglerBestillingsIdFunctionalException("qdist008 har mottatt forsendelse uten påkrevd bestillingsId");
		} else if (bestillingsId.trim().isEmpty()) {
			throw new ForsendelseManglerBestillingsIdFunctionalException("qdist008 har mottatt forsendelse med tom bestillingsId");
		}
		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);
		MDC.put(CALL_ID, exchange.getProperty(PROPERTY_BESTILLINGS_ID, String.class));
	}
}
