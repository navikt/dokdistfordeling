package no.nav.dokdistfordeling.exception.technical;

public class S3FailedToPutDocumentTechnicalException extends AbstractDokdistfordelingTechnicalException {
	public S3FailedToPutDocumentTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public S3FailedToPutDocumentTechnicalException(String message) {
		super(message);
	}
}
