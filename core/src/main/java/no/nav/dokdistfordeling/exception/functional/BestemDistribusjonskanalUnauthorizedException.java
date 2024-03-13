package no.nav.dokdistfordeling.exception.functional;

import no.nav.dokdistfordeling.exception.technical.BestemDistribusjonskanalTechnicalException;

public class BestemDistribusjonskanalUnauthorizedException extends BestemDistribusjonskanalTechnicalException {

	public BestemDistribusjonskanalUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}
}