package no.nav.dokdistfordeling.exception.technical;

public class S3FailedToGetDocumentTechnicalException extends AbstractDokdistfordelingTechnicalException {
	public S3FailedToGetDocumentTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public S3FailedToGetDocumentTechnicalException(String message) {
		super(message);
	}
}
