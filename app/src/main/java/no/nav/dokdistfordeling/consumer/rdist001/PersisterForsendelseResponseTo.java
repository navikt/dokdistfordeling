package no.nav.dokdistfordeling.consumer.rdist001;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class PersisterForsendelseResponseTo {
	private final String forsendelseId;
}
