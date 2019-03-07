package no.nav.dokdistfordeling.exception.technical;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class DokdistfordelingTechnicalException extends RuntimeException {

	public DokdistfordelingTechnicalException(String message) {
		super(message);
	}

	public DokdistfordelingTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
