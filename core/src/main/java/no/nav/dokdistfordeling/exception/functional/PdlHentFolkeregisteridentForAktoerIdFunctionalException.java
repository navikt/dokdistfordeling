package no.nav.dokdistfordeling.exception.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class PdlHentFolkeregisteridentForAktoerIdFunctionalException extends AbstractDokdistfordelingFunctionalException {
	public PdlHentFolkeregisteridentForAktoerIdFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdlHentFolkeregisteridentForAktoerIdFunctionalException(String message) {
		super(message);
	}
}
