package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagHentAdresseFunctionalException;

public class UkjentAdresseException extends RegoppslagHentAdresseFunctionalException {
    public UkjentAdresseException(String message, Throwable cause) {
        super(message, cause);
    }
}
