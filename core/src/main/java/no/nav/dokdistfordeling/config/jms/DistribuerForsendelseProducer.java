package no.nav.dokdistfordeling.config.jms;

import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;

public interface DistribuerForsendelseProducer {

	void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId, String journalpostId);
}
