package no.nav.dokdistfordeling.domain;

import lombok.Builder;

@Builder
public record Postadresse(
		String adressetype,
		String postnummer,
		String poststed,
		String adresselinje1,
		String adresselinje2,
		String adresselinje3,
		String land) {

	public static final String NORSK_POSTADRESSE = "NorskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "UtenlandskPostadresse";

	public boolean erNorskPostadresse() {
		return NORSK_POSTADRESSE.equalsIgnoreCase(adressetype);
	}

	public boolean erUtenlandskPostadresse() {
		return UTENLANDSK_POSTADRESSE.equalsIgnoreCase(adressetype);
	}
}
