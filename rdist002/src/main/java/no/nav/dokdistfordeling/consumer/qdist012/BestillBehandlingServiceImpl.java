package no.nav.dokdistfordeling.consumer.qdist012;


import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import org.springframework.stereotype.Component;

@Component
public class BestillBehandlingServiceImpl implements BestillBehandlingService {

	public String bestillJournalpostDistribusjon(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {

		String bestillingsId = "response bestillingsid coming from qdist012";

		return bestillingsId;
	}

}