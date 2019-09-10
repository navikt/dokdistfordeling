package no.nav.dokdistfordeling.nais.checks;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.consumer.aktoerregister.Aktoerregister;
import no.nav.dokdistfordeling.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistfordeling.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistfordeling.nais.selftest.DependencyType;
import no.nav.dokdistfordeling.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AktoerregisterCheck extends AbstractDependencyCheck {

	private final Aktoerregister aktoerregister;

	public AktoerregisterCheck(MeterRegistry registry, Aktoerregister aktoerregister, @Value("${aktoerregister.api.v1.url}") String aktoerregisterUrl) {
		super(DependencyType.REST, "aktoerregister", aktoerregisterUrl, Importance.WARNING, registry);
		this.aktoerregister = aktoerregister;
	}

	@Override
	protected void doCheck() {
		try {
			aktoerregister.hentIdentForAktoerId(null);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Kunne ikke kalle aktoerregister.", e);
		}
	}

}