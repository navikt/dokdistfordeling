package no.nav.dokdistfordeling.exception;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DokdistfordelingTechnicalException extends RuntimeException {

	public DokdistfordelingTechnicalException(String message) {
		super(message);
	}

	public DokdistfordelingTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
