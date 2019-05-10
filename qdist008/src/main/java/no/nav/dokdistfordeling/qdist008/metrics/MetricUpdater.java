package no.nav.dokdistfordeling.qdist008.metrics;

import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;
import static no.nav.dokdistfordeling.util.Qdist008Util.countVedlegg;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class MetricUpdater {

	private static final String QDIST008_SERVICE = "dok_business_counter";
	private static final String HOVEDDOKUMENT = "hoveddokument";
	private static final String VEDLEGG = "vedlegg";
	private static final String LABEL_TEMA = "tema";
	private static final String LABEL_DOKUMENTTYPEID = "dokumenttypeid";
	private static final String LABEL_BESTILLENDE_FAGSYSTEM = "bestillende_fagsystem";
	private static final String LABEL_TILKNYTNING = "tilknytning";

	private static MeterRegistry meterRegistry;

	@Inject
	public MetricUpdater(MeterRegistry meterRegistry) {
		MetricUpdater.meterRegistry = meterRegistry;
	}

	public static void updateQdist008Metrics( DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		meterRegistry.counter(QDIST008_SERVICE,
				LABEL_PROCESS, SERVICE_ID,
				LABEL_DOKUMENTTYPEID, getDokumenttypeIdHoveddokument(distribusjonbestilling),
				LABEL_TEMA, distribusjonbestilling.getTema(),
				LABEL_TILKNYTNING, HOVEDDOKUMENT,
				LABEL_BESTILLENDE_FAGSYSTEM, distribusjonbestilling.getBestillendeFagsystem()).increment();

		meterRegistry.counter(QDIST008_SERVICE,
				LABEL_PROCESS, SERVICE_ID,
				LABEL_DOKUMENTTYPEID, getDokumenttypeIdHoveddokument(distribusjonbestilling),
				LABEL_TEMA, distribusjonbestilling.getTema(),
				LABEL_TILKNYTNING, VEDLEGG,
				LABEL_BESTILLENDE_FAGSYSTEM, distribusjonbestilling.getBestillendeFagsystem())
				.increment(countVedlegg(distribusjonbestilling));
	}
}
