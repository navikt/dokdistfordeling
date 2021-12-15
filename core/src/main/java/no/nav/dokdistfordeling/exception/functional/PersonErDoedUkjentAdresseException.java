package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagHentAdresseFunctionalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.GONE)
public class PersonErDoedUkjentAdresseException extends RegoppslagHentAdresseFunctionalException {
    public PersonErDoedUkjentAdresseException(String message, Throwable cause) {
        super(message, cause);
    }
}
