package no.nav.dokdistfordeling.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends DokdistfordelingFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
