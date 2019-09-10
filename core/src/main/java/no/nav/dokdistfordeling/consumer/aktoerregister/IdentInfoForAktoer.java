package no.nav.dokdistfordeling.consumer.aktoerregister;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Value
@Builder
public class IdentInfoForAktoer {

	private final List<IdentInfo> identer;
	private final String feilmelding;

	@Value
	@Builder
	public static class IdentInfo {
		private final String ident;
		private final String identgruppe;
		private final Boolean gjeldende;
	}
}
