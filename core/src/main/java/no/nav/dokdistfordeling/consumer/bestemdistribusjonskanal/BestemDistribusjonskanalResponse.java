package no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal;

import lombok.Builder;

@Builder
public record BestemDistribusjonskanalResponse(
		String distribusjonskanal,
		String regel,
		String regelBegrunnelse) {
}