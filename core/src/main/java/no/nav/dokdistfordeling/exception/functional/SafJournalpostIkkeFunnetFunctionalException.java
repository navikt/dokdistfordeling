package no.nav.dokdistfordeling.exception.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class SafJournalpostIkkeFunnetFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public SafJournalpostIkkeFunnetFunctionalException(String message) {
		super(message);
	}
}
