package no.nav.dokdistfordeling.exception.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(value = UNAUTHORIZED)
public class BrukerManglerTilgangTilDokumentFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public BrukerManglerTilgangTilDokumentFunctionalException(String message) {
		super(message);
	}
}
