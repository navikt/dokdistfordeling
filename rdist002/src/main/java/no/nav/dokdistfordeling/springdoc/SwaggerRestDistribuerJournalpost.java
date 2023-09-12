package no.nav.dokdistfordeling.springdoc;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import no.nav.dokdistfordeling.DistribuerJournalpostResponseTo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "OK - journalposten distribueres og bestillingsId returneres.",
				content = @Content(mediaType = "application/json", schema = @Schema(implementation = DistribuerJournalpostResponseTo.class))),
		@ApiResponse(responseCode = "400", description = "Ugyldig input. Validering av request body, eller validering av journalposten som journalpostId refererer til feilet.", content = @Content),
		@ApiResponse(responseCode = "401", description = "* Bruker mangler tilgang for å vise journalposten.\n* Ugyldig OIDC token.", content = @Content),
		@ApiResponse(responseCode = "404", description = "Journalposten ble ikke funnet.", content = @Content),
		@ApiResponse(responseCode = "409", description = "Journalposten er allerede distribuert."),
		@ApiResponse(responseCode = "410", description = "Journalpost kan ikke distribueres. Bruker er død og har ukjent adresse."),
		@ApiResponse(responseCode = "500", description = "Teknisk feil under prosessering av forsendelse.", content = @Content)
}
)
public @interface SwaggerRestDistribuerJournalpost {
}
