package no.nav.dokdistfordeling.consumer.dokdist.rdist001;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class ForsendelseResponseTo {
	private final String forsendelseId;
}
