package no.nav.dokdistfordeling.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DokdistfordelingFunctionalException extends RuntimeException {

	public DokdistfordelingFunctionalException(String message) {
		super(message);
	}

	public DokdistfordelingFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
