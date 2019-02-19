package no.nav.dokdistfordeling.qdist008;

import lombok.Builder;
import lombok.Value;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Value
@Builder
public class DistribuerForsendelseTilSentralPrint {
	private final String bestillingsId;
}
