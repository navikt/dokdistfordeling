package no.nav.dokdistfordeling.exception;

import org.springframework.web.client.HttpClientErrorException;

public class PersisterForsendelseFunctionalException extends DokdistfordelingFunctionalException {
	public PersisterForsendelseFunctionalException(String message, HttpClientErrorException e) {
		super(message);
	}
}
