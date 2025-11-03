package no.nav.dokdistfordeling.consumer.regoppslag;

import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;

public class RegoppslagTechnicalException extends AbstractDokdistfordelingTechnicalException {

	public RegoppslagTechnicalException(String message) {
		super(message);
	}
}