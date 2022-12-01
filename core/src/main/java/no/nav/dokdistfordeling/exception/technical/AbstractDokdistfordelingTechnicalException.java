package no.nav.dokdistfordeling.exception.technical;

public abstract class AbstractDokdistfordelingTechnicalException extends RuntimeException {

	public AbstractDokdistfordelingTechnicalException(String message) {
		super(message);
	}

	public AbstractDokdistfordelingTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
