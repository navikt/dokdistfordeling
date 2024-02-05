package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(code = NOT_FOUND)
public class PdlHentFolkeregisteridentForAktoerIdFunctionalException extends AbstractDokdistfordelingFunctionalException {
	public PdlHentFolkeregisteridentForAktoerIdFunctionalException(String message) {
		super(message);
	}
}
