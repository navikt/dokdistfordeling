package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.domain.Adresse;

import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RegoppslagAdresseMapper {
    private static final String ISO_3166_ALPHA_2_NORGE = "NO";

    private RegoppslagAdresseMapper() {
    }

    public static Adresse map(HentMottakerOgAdresseResponseTo.AdresseTo regoppslagAdresseTo) {
        if (ISO_3166_ALPHA_2_NORGE.equals(regoppslagAdresseTo.getLandkode())) {
            return new Adresse(
                    NORSK_POSTADRESSE,
                    regoppslagAdresseTo.getPostnummer(),
                    regoppslagAdresseTo.getPoststed(),
                    isBlank(regoppslagAdresseTo.getAdresselinje1()) ? null : regoppslagAdresseTo.getAdresselinje1(),
                    regoppslagAdresseTo.getAdresselinje2(),
                    regoppslagAdresseTo.getAdresselinje3(),
                    regoppslagAdresseTo.getLandkode()
            );
        } else {
            return new Adresse(
                    UTENLANDSK_POSTADRESSE,
                    null,
                    null,
                    regoppslagAdresseTo.getAdresselinje1(),
                    regoppslagAdresseTo.getAdresselinje2(),
                    regoppslagAdresseTo.getAdresselinje3(),
                    regoppslagAdresseTo.getLandkode()
            );
        }
    }
}
