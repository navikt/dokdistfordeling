package no.nav.dokdistfordeling.exception.functional;

public class PdlPersonIkkeFunnetFunctionalException extends AbstractDokdistfordelingFunctionalException {
	public PdlPersonIkkeFunnetFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdlPersonIkkeFunnetFunctionalException(String message) {
		super(message);
	}
}
