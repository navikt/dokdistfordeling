package no.nav.dokdistfordeling.web;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestControllerAdvice
public class ApplicationServletExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String CONSUMER_TEKNISK_FEIL_MESSAGE = "Teknisk feil ved kall mot ekstern tjeneste med feilmelding=%s";
	private static final String UNAUTHORIZED_MESSAGE = "Token er ikke autorisert for denne tjenesten. " +
													   "Token må være utsted av NAV onprem security-token-service eller Entra Id. " +
													   "Vi har endret dette fra 25.07.2025, hvis dere plutselig mangler tilgang: ta kontakt i #team_dokumentløsninger";

	@ExceptionHandler({JwtTokenUnauthorizedException.class})
	public ResponseEntity<Object> handleUnauthorized(WebRequest webRequest) {
		log.error("System consumerId={}, sender token som ikke er autorisert. Dette kan være en tilgang som ikke er lagt til i config.", MDC.get(CONSUMER_ID));
		HttpStatus returnertHttpStatus = UNAUTHORIZED;

		return new ResponseEntity<>(formaterBody(returnertHttpStatus, UNAUTHORIZED_MESSAGE, webRequest), returnertHttpStatus);
	}

	@ExceptionHandler({CallNotPermittedException.class})
	ResponseEntity<Object> handleCallNotPermittedException(Exception ex, WebRequest webRequest) {
		var feilmelding = CONSUMER_TEKNISK_FEIL_MESSAGE.formatted(ex.getMessage());

		log.warn(feilmelding, ex);
		HttpStatus returnertHttpStatus = SERVICE_UNAVAILABLE;

		return new ResponseEntity<>(formaterBody(returnertHttpStatus, feilmelding, webRequest), returnertHttpStatus);
	}

	private Map<String, Object> formaterBody(HttpStatus httpStatus, String feilmelding, WebRequest webRequest) {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
		Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());

		body.put("status", httpStatus.value());
		body.put("error", httpStatus.getReasonPhrase());
		body.put("message", feilmelding);
		body.put("path", ((ServletWebRequest) webRequest).getRequest().getRequestURI());

		return body;
	}

}