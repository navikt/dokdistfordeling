package no.nav.dokdistfordeling.consumer.dokdistkanal;

import lombok.Builder;

@Builder
public record BestemDistribusjonskanalResponse(
		String distribusjonskanal,
		String regel,
		String regelBegrunnelse
) {
}