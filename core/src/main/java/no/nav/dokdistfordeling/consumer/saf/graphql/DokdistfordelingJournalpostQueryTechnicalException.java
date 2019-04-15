package no.nav.dokdistfordeling.consumer.saf.graphql;

import no.nav.dokdistfordeling.exception.technical.AbstractDokdistfordelingTechnicalException;

public class DokdistfordelingJournalpostQueryTechnicalException extends AbstractDokdistfordelingTechnicalException {

	public DokdistfordelingJournalpostQueryTechnicalException(String message) {
		super(message);
	}

	public DokdistfordelingJournalpostQueryTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
