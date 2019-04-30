package no.nav.dokdistfordeling.endpoints.swagger;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses(value = {
		@ApiResponse(code = 200, message = "OK - journalposten distribueres og bestillingsId returneres.", response = DistribuerJournalpostRequestTo.class),
		@ApiResponse(code = 400, message = "Ugyldig input. Validering av request body, eller validering av journalposten som journalpostId refererer til feilet."),
		@ApiResponse(code = 401, message = "* Bruker mangler tilgang for å vise journalposten.\n* Ugyldig OIDC token."),
		@ApiResponse(code = 404, message = "Journalposten ble ikke funnet."),
		@ApiResponse(code = 500, message = " Teknisk feil under prosessering av forsendelse.")
}
)
public @interface SwaggerRestDistribuerJournalpost {
	@AliasFor(
			annotation = RequestMapping.class
	)
	String value() default "";
}
