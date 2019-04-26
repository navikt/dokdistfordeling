package no.nav.dokdistfordeling.exception.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class SafJournalpostQueryUnauthorizedException extends AbstractDokdistfordelingFunctionalException {

	public SafJournalpostQueryUnauthorizedException(String message) {
		super(message);
	}

	public SafJournalpostQueryUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}
