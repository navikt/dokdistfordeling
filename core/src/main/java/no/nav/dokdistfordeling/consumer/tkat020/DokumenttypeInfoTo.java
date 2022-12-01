package no.nav.dokdistfordeling.consumer.tkat020;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DokumenttypeInfoTo {
	private final String dokumentTittel;
}
