package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class DokmetFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public DokmetFunctionalException(String message, Throwable e) {
		super(message, e);
	}
}