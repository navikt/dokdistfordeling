package no.nav.dokdistfordeling.nais.checks;


import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistfordeling.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistfordeling.nais.selftest.DependencyType;
import no.nav.dokdistfordeling.nais.selftest.Importance;
import no.nav.tjeneste.domene.brevogarkiv.arkiverdokumentproduksjon.v1.ArkiverDokumentproduksjonV1;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Component
public class Tjoark110Check extends AbstractDependencyCheck {

	private final ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1;

	@Inject
	public Tjoark110Check(MeterRegistry meterRegistry, ArkiverDokumentproduksjonV1 arkiverDokumentproduksjonV1, ArkiverDokumentproduksjonV1Alias arkiverDokumentproduksjonV1Alias) {
		super(DependencyType.SOAP, "tjoark110", arkiverDokumentproduksjonV1Alias.getEndpointurl(), Importance.WARNING, meterRegistry);
		this.arkiverDokumentproduksjonV1 = arkiverDokumentproduksjonV1;
	}

	@Override
	protected void doCheck() {
		try {
			arkiverDokumentproduksjonV1.ping();
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Kunne ikke pinge tjoark110", e);
		}
	}

}
