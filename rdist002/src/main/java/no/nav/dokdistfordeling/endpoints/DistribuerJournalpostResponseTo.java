package no.nav.dokdistfordeling.endpoints;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;

@Value
@ApiModel(value = "DistribuerJournalpostResponseTo model")
public class DistribuerJournalpostResponseTo {
	@ApiModelProperty(name = "bestillingsId", value = "GUID generert av tjenesten som unikt identifiserer distribusjonsbestillingen\n", example = "3ea4d118-6012-4fd0-9095-0f9944568d03")
	private final String bestillingsId;
}
