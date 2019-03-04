package no.nav.dokdistfordeling.exception;

import org.springframework.web.client.HttpServerErrorException;

public class PersisterForsendelseTechnicalException extends DokdistfordelingFunctionalException {
	public PersisterForsendelseTechnicalException(String message, HttpServerErrorException e) {
		super(message);
	}
}
