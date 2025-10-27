package no.nav.dokdistfordeling.consumer.regoppslag;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class RegoppslagFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public RegoppslagFunctionalException(String message) {
		super(message);
	}
}