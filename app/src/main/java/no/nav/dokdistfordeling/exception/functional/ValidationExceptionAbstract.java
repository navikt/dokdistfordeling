package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class ValidationExceptionAbstract extends AbstractDokdistfordelingFunctionalException {

	public ValidationExceptionAbstract(String message) {
		super(message);
	}

	public ValidationExceptionAbstract(String message, Throwable cause) {
		super(message, cause);
	}
}
