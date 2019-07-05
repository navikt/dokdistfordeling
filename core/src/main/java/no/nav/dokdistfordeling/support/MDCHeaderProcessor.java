package no.nav.dokdistfordeling.support;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * @author Erik Bråten, Visma Consulting.
 */
public class MDCHeaderProcessor implements Processor {

	@Override
	public void process(Exchange exchange) {
		setCallIdToMdc(exchange);
		setConsumerIdToMdc(exchange);
	}

	private void setCallIdToMdc(Exchange exchange) {
		String callId = exchange.getIn().getHeader(CALL_ID, String.class);
		if (callId == null || callId.isEmpty()) {
			callId = UUID.randomUUID().toString();
			exchange.getIn().setHeader(CALL_ID, callId);
		}
		MDC.put(CALL_ID, callId);
	}

	private void setConsumerIdToMdc(Exchange exchange) {
		String consumerId = exchange.getIn().getHeader(CONSUMER_ID, String.class);
		if (consumerId != null && !consumerId.isEmpty()) {
			MDC.put(CONSUMER_ID, consumerId);
		}
	}
}
