package no.nav.dokdistfordeling.config.jms;

import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;

public interface Producer {

	void produce(HentDokumenterFraJoark hentDokumenterFraJoark);
}
