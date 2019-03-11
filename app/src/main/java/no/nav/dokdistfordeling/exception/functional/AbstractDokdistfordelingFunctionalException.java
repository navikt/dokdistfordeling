package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class AbstractDokdistfordelingFunctionalException extends RuntimeException {

	public AbstractDokdistfordelingFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistfordelingFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
