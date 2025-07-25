package no.nav.dokdistfordeling.web;

import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class ApplicationServletExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String UNAUTHORIZED_MESSAGE = "Token er ikke autorisert for denne tjenesten. " +
													   "Token må være utsted av NAV onprem security-token-service eller Entra Id. " +
													   "Vi har endret dette fra 25.07.2025, hvis dere plutselig mangler tilgang: ta kontakt i #team_dokumentløsninger";

	@ExceptionHandler({JwtTokenUnauthorizedException.class})
	public ResponseEntity<Object> handleUnauthorized(WebRequest webRequest) {
		DefaultErrorAttributes errorAttributes = new DefaultErrorAttributes();
		Map<String, Object> body = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
		body.put("status", UNAUTHORIZED.value());
		body.put("error", UNAUTHORIZED.getReasonPhrase());
		body.put("message", UNAUTHORIZED_MESSAGE);
		body.put("path", ((ServletWebRequest) webRequest).getRequest().getRequestURI());
		return new ResponseEntity<>(body, UNAUTHORIZED);
	}
}
