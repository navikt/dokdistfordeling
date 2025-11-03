package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class PdlFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public PdlFunctionalException(String message) {
		super(message);
	}
}