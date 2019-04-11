package no.nav.dokdistfordeling.consumer.aktoerv2;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class HentIdentForAktoerIdResponseTo {

	private final String foedselsnr;

}
