package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.exception.technical.DokdistkanalTechnicalException;

public class DokdistkanalUnauthorizedException extends DokdistkanalTechnicalException {

	public DokdistkanalUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}