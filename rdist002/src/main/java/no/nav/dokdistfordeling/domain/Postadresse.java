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

	public static final String NORSK_POSTADRESSE = "norskPostadresse";
	public static final String UTENLANDSK_POSTADRESSE = "utenlandskPostadresse";
	public static final String LANDKODE_NORGE = "NO";

	public boolean erNorskPostadresse() {
		return NORSK_POSTADRESSE.equals(adressetype);
	}

	public boolean erUtenlandskPostadresse() {
		return UTENLANDSK_POSTADRESSE.equals(adressetype);
	}

	public static Postadresse norsk(String postnummer,
									String poststed,
									String adresselinje1,
									String adresselinje2,
									String adresselinje3) {
		return new Postadresse(
				NORSK_POSTADRESSE,
				postnummer,
				poststed,
				adresselinje1,
				adresselinje2,
				adresselinje3,
				LANDKODE_NORGE
		);
	}

	public static Postadresse utenlandsk(String adresselinje1,
										 String adresselinje2,
										 String adresselinje3,
										 String land) {
		return new Postadresse(
				UTENLANDSK_POSTADRESSE,
				null,
				null,
				adresselinje1,
				adresselinje2,
				adresselinje3,
				land
		);
	}
}
