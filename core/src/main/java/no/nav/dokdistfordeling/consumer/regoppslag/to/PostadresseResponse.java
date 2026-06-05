package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
public record PostadresseResponse(
		String navn,
		Adresse adresse
) {

	@Builder
	public record Adresse(
			PostadresseType type,
			String adresselinje1,
			String adresselinje2,
			String adresselinje3,
			String postnummer,
			String poststed,
			String landkode,
			String land
	) {
	}

	@Getter
	@AllArgsConstructor
	public enum PostadresseType {
		NORSKPOSTADRESSE("NorskPostadresse"),
		UTENLANDSKPOSTADRESSE("UtenlandskPostadresse");

		private final String navn;
	}
}
