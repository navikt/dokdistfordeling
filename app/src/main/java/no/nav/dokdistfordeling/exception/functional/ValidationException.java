package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationException extends AbstractDokdistfordelingFunctionalException {

	public ValidationException(String message) {
		super(message);
	}

	public ValidationException(String message, Throwable cause) {
		super(message, cause);
	}
}
