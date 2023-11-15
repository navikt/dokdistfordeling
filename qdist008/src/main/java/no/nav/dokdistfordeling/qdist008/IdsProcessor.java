package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.exception.functional.ForsendelseManglerBestillingsIdFunctionalException;
import no.nav.dokdistfordeling.support.MDCHeaderProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.language.xpath.XPathBuilder;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_BESTILLINGS_ID;

public class IdsProcessor extends MDCHeaderProcessor {

	@Override
	public void process(Exchange exchange) {
		super.process(exchange);
		setBestillingsIdAsProperty(exchange);
	}

	private void setBestillingsIdAsProperty(Exchange exchange) {
		String bestillingsId = XPathBuilder.xpath("//bestillingsId/text()").evaluate(exchange, String.class);

		if (bestillingsId.trim().isEmpty()) {
			throw new ForsendelseManglerBestillingsIdFunctionalException("qdist008 har mottatt forsendelse med tom bestillingsId");
		}

		exchange.setProperty(PROPERTY_BESTILLINGS_ID, bestillingsId);
	}
}
