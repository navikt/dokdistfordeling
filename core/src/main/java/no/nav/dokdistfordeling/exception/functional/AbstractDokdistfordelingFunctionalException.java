package no.nav.dokdistfordeling.exception.functional;

public abstract class AbstractDokdistfordelingFunctionalException extends RuntimeException {

	public AbstractDokdistfordelingFunctionalException(String message) {
		super(message);
	}

	public AbstractDokdistfordelingFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
