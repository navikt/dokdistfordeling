package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
public class RegoppslagAdresseMapper {
    private static final String ISO_3166_ALPHA_2_NORGE = "NO";
    private static final String TEST_VALUE = "";

    DistribuerJournalpostRequestTo.AdresseTo mapAdresseTo(final HentMottakerOgAdresseResponseTo.AdresseTo regoppslagAdresseTo) {
        if (ISO_3166_ALPHA_2_NORGE.equals(regoppslagAdresseTo.getLandkode())) {
            return DistribuerJournalpostRequestTo.AdresseTo.builder()
                    .adresselinje1(StringUtils.isBlank(regoppslagAdresseTo.getAdresselinje1())? null : regoppslagAdresseTo.getAdresselinje1())
                    .adresselinje2(regoppslagAdresseTo.getAdresselinje2())
                    .adresselinje3(regoppslagAdresseTo.getAdresselinje3())
                    .postnummer(regoppslagAdresseTo.getPostnummer())
                    .poststed(regoppslagAdresseTo.getPoststed())
                    .land(regoppslagAdresseTo.getLandkode())
                    .adressetype(NORSK_POSTADRESSE)
                    .build();
        } else {
            return DistribuerJournalpostRequestTo.AdresseTo.builder()
                    .adresselinje1(regoppslagAdresseTo.getAdresselinje1())
                    .adresselinje2(regoppslagAdresseTo.getAdresselinje2())
                    .adresselinje3(regoppslagAdresseTo.getAdresselinje3())
                    .land(regoppslagAdresseTo.getLandkode())
                    .adressetype(UTENLANDSK_POSTADRESSE)
                    .build();
        }
    }
}
