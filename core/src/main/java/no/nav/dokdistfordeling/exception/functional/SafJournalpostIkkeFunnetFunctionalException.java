package no.nav.dokdistfordeling.exception.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class SafJournalpostIkkeFunnetFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public SafJournalpostIkkeFunnetFunctionalException(String message) {
		super(message);
	}

	public SafJournalpostIkkeFunnetFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
