package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseResponseTo {
	AdresseTo adresse;

	@Value
	@Builder
	public static class AdresseTo {
		String adresselinje1;
		String adresselinje2;
		String adresselinje3;
		String postnummer;
		String poststed;
		String landkode;
	}
}
