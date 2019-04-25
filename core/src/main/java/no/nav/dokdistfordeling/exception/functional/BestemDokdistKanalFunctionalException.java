package no.nav.dokdistfordeling.exception.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BestemDokdistKanalFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public BestemDokdistKanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
