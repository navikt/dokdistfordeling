package no.nav.dokdistfordeling.consumer.rdist001;

import no.nav.dokdistfordeling.consumer.rdist001.domain.OppdaterForsendelseRequest;

public interface AdministrerForsendelse {

	String opprettForsendelse(final OpprettForsendelseRequestTo opprettForsendelseRequestTo);

	void oppdaterForsendelse(OppdaterForsendelseRequest oppdaterForsendelseRequest);
}
