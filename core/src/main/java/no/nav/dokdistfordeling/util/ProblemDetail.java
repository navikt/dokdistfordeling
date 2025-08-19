package no.nav.dokdistfordeling.util;

/**
 * ProblemDetail er en custom-implementasjon av Spring sin ProblemDetail klasse for lettere å kunne hente ut error message.
 */
public class ProblemDetail extends org.springframework.http.ProblemDetail {
	public String getErrorMessage(){
		return (String)getProperties().get("message");
	}
}
