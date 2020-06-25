package no.nav.dokdistfordeling.consumer.regoppslag;


import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public interface Regoppslag {
    HentMottakerOgAdresseResponseTo.AdresseTo hentOrganisasjonAdresse(final String orgnummer);
    HentMottakerOgAdresseResponseTo.AdresseTo hentPersonAdresse(final String foedselsnummer);
}