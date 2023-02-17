package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class DokMetDokumenttypeInfoFunctionalException extends AbstractDokdistfordelingFunctionalException {
	public DokMetDokumenttypeInfoFunctionalException(String message, Throwable e) {
		super(message, e);
	}
}
