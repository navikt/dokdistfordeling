package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.Builder;

import java.util.Set;

@Builder
public record PostadresseRequest(
		String ident,
		Set<String> filtrerAdressebeskyttelse) {
}
