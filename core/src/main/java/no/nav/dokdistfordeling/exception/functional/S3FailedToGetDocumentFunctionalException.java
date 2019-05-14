package no.nav.dokdistfordeling.exception.functional;

public class S3FailedToGetDocumentFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public S3FailedToGetDocumentFunctionalException(String message) {
		super(message);
	}
	public S3FailedToGetDocumentFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
