package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagHentAdresseFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class UkjentAdresseException extends RegoppslagHentAdresseFunctionalException {

	public UkjentAdresseException(String message, Throwable cause) {
		super(message, cause);
	}
}
