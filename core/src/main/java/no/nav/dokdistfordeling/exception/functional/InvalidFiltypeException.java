package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class InvalidFiltypeException extends AbstractDokdistfordelingFunctionalException {
	public InvalidFiltypeException(String message) {
		super(message);
	}
}
