package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostIdempotencyHandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
public class DistribuerJournalpostTransaksjon {

	private final DistribuerJournalpostIdempotencyHandler idempotencyHandler;
	private final DistribuerForsendelseProducer distribuerForsendelseProducer;

	public DistribuerJournalpostTransaksjon(DistribuerJournalpostIdempotencyHandler idempotencyHandler,
											DistribuerForsendelseProducer distribuerForsendelseProducer) {
		this.idempotencyHandler = idempotencyHandler;
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void distribuer(Long journalpostId, String bestillingsId, HentDokumenterFraJoark melding) {
		idempotencyHandler.opprettDistribuerJournalpostInfo(journalpostId, bestillingsId);
		distribuerForsendelseProducer.produce(melding, bestillingsId, journalpostId.toString());
	}
}
