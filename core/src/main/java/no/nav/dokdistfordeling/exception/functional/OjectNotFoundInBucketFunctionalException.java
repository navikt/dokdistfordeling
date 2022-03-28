package no.nav.dokdistfordeling.exception.functional;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public class OjectNotFoundInBucketFunctionalException extends AbstractDokdistfordelingFunctionalException {

	public OjectNotFoundInBucketFunctionalException(String message) {
		super(message);
	}

	public OjectNotFoundInBucketFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
