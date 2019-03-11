package no.nav.dokdistfordeling.exception.technical;

public class S3FailedToGetDocumentTechnicalExceptionAbstract extends AbstractDokdistfordelingTechnicalException {
	public S3FailedToGetDocumentTechnicalExceptionAbstract(String message, Throwable cause) {
		super(message, cause);
	}

	public S3FailedToGetDocumentTechnicalExceptionAbstract(String message) {
		super(message);
	}
}
