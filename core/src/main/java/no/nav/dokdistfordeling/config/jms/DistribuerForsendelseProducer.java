package no.nav.dokdistfordeling.config.jms;

import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;

public interface DistribuerForsendelseProducer {

	void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId);
}
