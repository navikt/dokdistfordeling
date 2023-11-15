package no.nav.dokdistfordeling.exception.technical;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ResponseStatus(INTERNAL_SERVER_ERROR)
public class SafUkjentErrorCodeException extends AbstractDokdistfordelingTechnicalException {

	public SafUkjentErrorCodeException(String message) {
		super(message);
	}
}
