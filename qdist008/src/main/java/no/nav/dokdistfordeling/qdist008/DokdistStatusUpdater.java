package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.consumer.dokdistadmin.DokdistadminConsumer;
import no.nav.dokdistfordeling.consumer.dokdistadmin.OppdaterForsendelseRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;

@Component
public class DokdistStatusUpdater {

	private static final String FORSENDELSE_STATUS_KLAR_FOR_DIST = "KLAR_FOR_DIST";

	private final DokdistadminConsumer dokdistadminConsumer;

	public DokdistStatusUpdater(DokdistadminConsumer dokdistadminConsumer) {
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);

		if (!isDistribusjonKanalLokalPrintOrIngenDistribusjon(exchange)) {
			dokdistadminConsumer.oppdaterForsendelse(new OppdaterForsendelseRequest(Long.valueOf(forsendelseId), FORSENDELSE_STATUS_KLAR_FOR_DIST));
		}
	}

	private boolean isDistribusjonKanalLokalPrintOrIngenDistribusjon(Exchange exchange) {
		return LOKAL_PRINT.equals(exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL)) ||
				INGEN_DISTRIBUSJON.equals(exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL));
	}

}