package no.nav.dokdistfordeling.exception.functional;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class BrukerManglerTilgangTilDokumentFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public BrukerManglerTilgangTilDokumentFunctionalException(String message) {
		super(message);
	}
}
