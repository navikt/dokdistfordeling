package no.nav.dokdistfordeling.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistfordeling.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistfordeling.nais.selftest.DependencyType;
import no.nav.dokdistfordeling.nais.selftest.Importance;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktoerV2Check extends AbstractDependencyCheck {

	private final AktoerV2 aktoerV2;

	public AktoerV2Check(MeterRegistry registry, AktoerV2 aktoerV2, @Value("${aktoer.v2.endpointurl}") String aktoerV2Url) {
		super(DependencyType.SOAP, "aktoerV2", aktoerV2Url, Importance.WARNING, registry);
		this.aktoerV2 = aktoerV2;
	}

	@Override
	protected void doCheck() {
		try {
			aktoerV2.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Kunne ikke pinge aktoerV2", e);
		}
	}


}
