package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.regoppslag.to.PostadresseResponse;
import no.nav.dokdistfordeling.domain.Postadresse;

import static no.nav.dokdistfordeling.domain.Postadresse.LANDKODE_NORGE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RegoppslagAdresseMapper {

    private RegoppslagAdresseMapper() {
    }

    public static Postadresse map(PostadresseResponse.Adresse postadresse) {
        if (LANDKODE_NORGE.equals(postadresse.landkode())) {
            return Postadresse.norsk(
					postadresse.postnummer(),
					postadresse.poststed(),
					isBlank(postadresse.adresselinje1()) ? null : postadresse.adresselinje1(),
					postadresse.adresselinje2(),
					postadresse.adresselinje3()
			);
        } else {
            return Postadresse.utenlandsk(
                    postadresse.adresselinje1(),
                    postadresse.adresselinje2(),
                    postadresse.adresselinje3(),
                    postadresse.landkode()
            );
        }
    }
}
