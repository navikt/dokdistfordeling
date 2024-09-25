package no.nav.dokdistfordeling;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Value
@Schema
public class DistribuerJournalpostResponseTo {
	@Schema(
			description = "GUID generert av tjenesten som unikt identifiserer distribusjonsbestillingen\n",
			requiredMode = REQUIRED,
			example = "3ea4d118-6012-4fd0-9095-0f9944568d03"
	)
	String bestillingsId;
}
