package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseRequestTo {
	private final String identifikator;
	private final String type;
}
