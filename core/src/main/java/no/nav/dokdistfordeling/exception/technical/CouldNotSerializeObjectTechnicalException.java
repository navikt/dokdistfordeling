package no.nav.dokdistfordeling.exception.technical;

/**
 * @author Sigurd Midttun, Visma Consulting
 */
public class CouldNotSerializeObjectTechnicalException extends AbstractDokdistfordelingTechnicalException {

	public CouldNotSerializeObjectTechnicalException(String message) {
		super(message);
	}

	public CouldNotSerializeObjectTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
