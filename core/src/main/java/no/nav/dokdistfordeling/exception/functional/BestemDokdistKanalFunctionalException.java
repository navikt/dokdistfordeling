package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class BestemDokdistKanalFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public BestemDokdistKanalFunctionalException(String message) {
		super(message);
	}

	public BestemDokdistKanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
