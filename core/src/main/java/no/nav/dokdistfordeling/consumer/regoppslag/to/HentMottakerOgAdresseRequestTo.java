package no.nav.dokdistfordeling.consumer.regoppslag.to;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HentMottakerOgAdresseRequestTo {
	String identifikator;
	String type;
	String tema;
}
