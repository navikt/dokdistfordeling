package no.nav.dokdistfordeling.consumer.dokdistkanal;

import lombok.Builder;

@Builder
public record BestemDistribusjonskanalRequest(
		String brukerId,
		String dokumenttypeId,
		boolean erArkivert,
		Integer forsendelseStoerrelse,
		String mottakerId,
		String tema,
		Integer antallDokumenter
) {
}