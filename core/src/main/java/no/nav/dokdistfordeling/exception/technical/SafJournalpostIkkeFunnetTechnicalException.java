package no.nav.dokdistfordeling.exception.technical;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(value = NOT_FOUND)
public class SafJournalpostIkkeFunnetTechnicalException extends AbstractDokdistfordelingTechnicalException {

	public SafJournalpostIkkeFunnetTechnicalException(String message) {
		super(message);
	}
}
