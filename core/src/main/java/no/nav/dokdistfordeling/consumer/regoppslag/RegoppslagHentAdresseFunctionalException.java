package no.nav.dokdistfordeling.consumer.regoppslag;


import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class RegoppslagHentAdresseFunctionalException extends AbstractDokdistfordelingFunctionalException {
	public RegoppslagHentAdresseFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
