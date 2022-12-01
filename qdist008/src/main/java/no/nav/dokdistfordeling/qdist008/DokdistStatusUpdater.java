package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_BESTILLINGS_ID;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;

import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Component;

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
		final String bestillingsId = exchange.getProperty(PROPERTY_BESTILLINGS_ID, String.class);

		if(shouldUpdate(exchange)){
			administrerForsendelse.oppdaterForsendelseStatus(forsendelseId, FORSENDELSE_STATUS_KLAR_FOR_DIST, bestillingsId);
		}
	}

	private boolean shouldUpdate(Exchange exchange){
		if(exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL).equals(DistribusjonsKanalCode.LOKAL_PRINT)||
				exchange.getProperty(PROPERTY_DISTRIBUSJONSKANAL).equals(DistribusjonsKanalCode.INGEN_DISTRIBUSJON)) {
			return false;
		}else{
			return true;
		}
	}
}
