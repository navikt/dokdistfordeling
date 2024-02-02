package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.domain.OppdaterForsendelseRequest;
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
	private final AdministrerForsendelse administrerForsendelse;

	public DokdistStatusUpdater(AdministrerForsendelse administrerForsendelse) {
		this.administrerForsendelse = administrerForsendelse;
	}

	@Handler
	public void doUpdate(Exchange exchange) {
		final String forsendelseId = exchange.getProperty(PROPERTY_FORSENDELSE_ID, String.class);

		if (!isDistribusjonKanalLokalPrintOrIngenDistribusjon(exchange)) {
			administrerForsendelse.oppdaterForsendelse(new OppdaterForsendelseRequest(Long.valueOf(forsendelseId), FORSENDELSE_STATUS_KLAR_FOR_DIST));
		}
	}

	private boolean isDistribusjonKanalLokalPrintOrIngenDistribusjon(Exchange exchange) {
		return LOKAL_PRINT.equals(exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL)) ||
				INGEN_DISTRIBUSJON.equals(exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL));
	}
}
