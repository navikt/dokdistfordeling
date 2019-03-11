package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class DocumentNotFoundInS3FunctionalExceptionAbstract extends AbstractDokdistfordelingFunctionalException {

	public DocumentNotFoundInS3FunctionalExceptionAbstract(String message) {
		super(message);
	}

	public DocumentNotFoundInS3FunctionalExceptionAbstract(String message, Throwable cause) {
		super(message, cause);
	}
}
