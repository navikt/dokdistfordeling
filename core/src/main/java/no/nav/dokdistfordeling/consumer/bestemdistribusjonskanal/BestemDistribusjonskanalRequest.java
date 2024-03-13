package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.Builder;

@Builder
public record BestemDistribusjonskanalRequest(
		String brukerId,
		String dokumenttypeId,
		boolean erArkivert,
		Integer forsendelseStoerrelse,
		String mottakerId,
		String tema) {
}