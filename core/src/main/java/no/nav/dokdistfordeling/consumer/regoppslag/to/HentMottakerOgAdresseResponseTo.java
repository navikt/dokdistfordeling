package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseResponseTo {
	private final AdresseTo adresse;

	@Value
	@Builder
	public static class AdresseTo {
		private final String adresselinje1;
		private final String adresselinje2;
		private final String adresselinje3;
		private final String postnummer;
		private final String poststed;
		private final String landkode;
	}
}
