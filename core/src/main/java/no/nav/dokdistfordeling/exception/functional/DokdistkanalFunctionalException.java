package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class DokdistkanalFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public DokdistkanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}