package no.nav.dokdistfordeling.config.jms;

import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;

public interface HentDokumenterFraJoarkProducer {

	void produce(HentDokumenterFraJoark hentDokumenterFraJoark);
}
