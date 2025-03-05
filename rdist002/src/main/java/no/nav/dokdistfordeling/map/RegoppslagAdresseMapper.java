package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.domain.Postadresse;

import static no.nav.dokdistfordeling.domain.Postadresse.LANDKODE_NORGE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RegoppslagAdresseMapper {

    private RegoppslagAdresseMapper() {
    }

    public static Postadresse map(HentMottakerOgAdresseResponseTo.AdresseTo regoppslagAdresseTo) {
        if (LANDKODE_NORGE.equals(regoppslagAdresseTo.getLandkode())) {
            return Postadresse.norsk(
					regoppslagAdresseTo.getPostnummer(),
					regoppslagAdresseTo.getPoststed(),
					isBlank(regoppslagAdresseTo.getAdresselinje1()) ? null : regoppslagAdresseTo.getAdresselinje1(),
					regoppslagAdresseTo.getAdresselinje2(),
					regoppslagAdresseTo.getAdresselinje3()
			);
        } else {
            return Postadresse.utenlandsk(
                    regoppslagAdresseTo.getAdresselinje1(),
                    regoppslagAdresseTo.getAdresselinje2(),
                    regoppslagAdresseTo.getAdresselinje3(),
                    regoppslagAdresseTo.getLandkode()
            );
        }
    }
}
