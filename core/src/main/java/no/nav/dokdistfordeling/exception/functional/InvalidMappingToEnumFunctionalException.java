package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class InvalidMappingToEnumFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public InvalidMappingToEnumFunctionalException(String message) {
		super(message);
	}
}