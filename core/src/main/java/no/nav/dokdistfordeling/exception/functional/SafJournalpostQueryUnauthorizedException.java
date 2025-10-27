package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(UNAUTHORIZED)
public class SafJournalpostQueryUnauthorizedException extends AbstractDokdistfordelingFunctionalException {

	public SafJournalpostQueryUnauthorizedException(String message) {
		super(message);
	}

	public SafJournalpostQueryUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}