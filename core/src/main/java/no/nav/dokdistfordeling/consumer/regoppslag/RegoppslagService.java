package no.nav.dokdistfordeling.consumer.regoppslag;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
class RegoppslagService implements Regoppslag {
    private static final String ORGANISASJON_TYPE = "ORGANISASJON";
    private static final String PERSON_TYPE = "PERSON";

    private final RegoppslagRestConsumer regoppslagRestConsumer;

    @Autowired
    RegoppslagService(RegoppslagRestConsumer regoppslagRestConsumer) {
        this.regoppslagRestConsumer = regoppslagRestConsumer;
    }

    @Override
    public HentMottakerOgAdresseResponseTo.AdresseTo hentOrganisasjonAdresse(String orgnummer) {
        return regoppslagRestConsumer.hentAdresse(HentMottakerOgAdresseRequestTo.builder()
                .identifikator(orgnummer)
                .type(ORGANISASJON_TYPE)
                .build());
    }

    @Override
    public HentMottakerOgAdresseResponseTo.AdresseTo hentPersonAdresse(String foedselsnummer, String tema) {
        return regoppslagRestConsumer.hentAdresse(HentMottakerOgAdresseRequestTo.builder()
                .identifikator(foedselsnummer)
                .type(PERSON_TYPE)
                .tema(tema)
                .build());
    }
}
