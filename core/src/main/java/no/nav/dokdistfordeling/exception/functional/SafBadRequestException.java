package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class SafBadRequestException extends AbstractDokdistfordelingFunctionalException {

	public SafBadRequestException(String message) {
		super(message);
	}
}
