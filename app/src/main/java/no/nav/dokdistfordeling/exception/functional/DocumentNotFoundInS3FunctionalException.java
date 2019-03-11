package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DocumentNotFoundInS3FunctionalException extends AbstractDokdistfordelingFunctionalException {

	public DocumentNotFoundInS3FunctionalException(String message) {
		super(message);
	}

	public DocumentNotFoundInS3FunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
