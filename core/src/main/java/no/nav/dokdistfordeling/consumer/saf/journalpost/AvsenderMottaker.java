package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AvsenderMottaker {
	private final String id;
	private final String navn;
}
