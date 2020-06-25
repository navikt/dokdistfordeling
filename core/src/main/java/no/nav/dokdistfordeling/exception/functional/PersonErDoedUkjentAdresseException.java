package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagHentAdresseFunctionalException;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public class PersonErDoedUkjentAdresseException extends RegoppslagHentAdresseFunctionalException {
    public PersonErDoedUkjentAdresseException(String message, Throwable cause) {
        super(message, cause);
    }
}
