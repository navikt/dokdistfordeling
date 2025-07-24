package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(value = UNAUTHORIZED)
public class UnauthorizedException extends AbstractDokdistfordelingFunctionalException {
	public UnauthorizedException(String message) {
		super(message);
	}
}
