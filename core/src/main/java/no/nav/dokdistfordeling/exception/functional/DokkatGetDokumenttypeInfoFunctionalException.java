package no.nav.dokdistfordeling.exception.functional;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DokkatGetDokumenttypeInfoFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public DokkatGetDokumenttypeInfoFunctionalException(String message) {
		super(message);
	}

	public DokkatGetDokumenttypeInfoFunctionalException(String message, Throwable e) {
		super(message, e);
	}
}
