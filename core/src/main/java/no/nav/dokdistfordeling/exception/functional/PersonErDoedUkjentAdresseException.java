package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.GONE;

@ResponseStatus(GONE)
public class PersonErDoedUkjentAdresseException extends RegoppslagFunctionalException {

	public PersonErDoedUkjentAdresseException(String message) {
		super(message);
	}
}