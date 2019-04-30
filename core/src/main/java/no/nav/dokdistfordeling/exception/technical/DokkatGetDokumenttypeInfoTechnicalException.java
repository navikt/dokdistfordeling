package no.nav.dokdistfordeling.exception.technical;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DokkatGetDokumenttypeInfoTechnicalException extends AbstractDokdistfordelingTechnicalException {
	public DokkatGetDokumenttypeInfoTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DokkatGetDokumenttypeInfoTechnicalException(String message) {
		super(message);
	}
}
