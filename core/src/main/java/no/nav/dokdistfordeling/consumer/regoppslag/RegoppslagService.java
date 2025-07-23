package no.nav.dokdistfordeling.consumer.regoppslag;

import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseRequestTo;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import org.springframework.stereotype.Component;

@Component
public class RegoppslagService {
    private static final String ORGANISASJON_TYPE = "ORGANISASJON";
    private static final String PERSON_TYPE = "PERSON";

    private final RegoppslagRestConsumer regoppslagRestConsumer;

    RegoppslagService(RegoppslagRestConsumer regoppslagRestConsumer) {
        this.regoppslagRestConsumer = regoppslagRestConsumer;
    }

    public HentMottakerOgAdresseResponseTo.AdresseTo hentOrganisasjonAdresse(String orgnummer) {
        return regoppslagRestConsumer.hentAdresse(HentMottakerOgAdresseRequestTo.builder()
                .identifikator(orgnummer)
                .type(ORGANISASJON_TYPE)
                .build());
    }

    public HentMottakerOgAdresseResponseTo.AdresseTo hentPersonAdresse(String foedselsnummer, String tema) {
        return regoppslagRestConsumer.hentAdresse(HentMottakerOgAdresseRequestTo.builder()
                .identifikator(foedselsnummer)
                .type(PERSON_TYPE)
                .tema(tema)
                .build());
    }
}
