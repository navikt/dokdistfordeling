package no.nav.dokdistfordeling.dokdistdb;

import lombok.Builder;

@Builder
public record DistribuerJournalpostInfoResponse(
		Long journalpostId,
		String bestillingsId) {
}
