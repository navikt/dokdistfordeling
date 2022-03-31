package no.nav.dokdistfordeling.exception.technical;

public class FailedBucketUploadTechnicalException extends AbstractDokdistfordelingTechnicalException {
	public FailedBucketUploadTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public FailedBucketUploadTechnicalException(String message) {
		super(message);
	}
}
