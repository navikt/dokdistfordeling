package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.regoppslag.to.PostadresseResponse;
import no.nav.dokdistfordeling.domain.Postadresse;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RegoppslagAdresseMapper {

	private RegoppslagAdresseMapper() {
	}

	public static Postadresse map(PostadresseResponse.Adresse postadresse) {
		return switch (postadresse.type()) {
			case NORSKPOSTADRESSE ->
					Postadresse.builder()
							.adressetype(postadresse.type().name())
							.postnummer(postadresse.postnummer())
							.poststed(postadresse.poststed())
							.adresselinje1(isBlank(postadresse.adresselinje1()) ? null : postadresse.adresselinje1())
							.adresselinje2(postadresse.adresselinje2())
							.adresselinje3(postadresse.adresselinje3())
							.land(postadresse.landkode())
							.build();

			case UTENLANDSKPOSTADRESSE ->
					Postadresse.builder()
							.adressetype(postadresse.type().name())
							.adresselinje1(postadresse.adresselinje1())
							.adresselinje2(postadresse.adresselinje2())
							.adresselinje3(postadresse.adresselinje3())
							.land(postadresse.landkode())
							.build();
		};
	}
}
