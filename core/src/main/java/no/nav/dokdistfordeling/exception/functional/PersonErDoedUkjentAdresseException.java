package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagHentAdresseFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.GONE;

@ResponseStatus(value = GONE)
public class PersonErDoedUkjentAdresseException extends RegoppslagHentAdresseFunctionalException {
public PersonErDoedUkjentAdresseException(String message) {
		super(message);
	}
}
