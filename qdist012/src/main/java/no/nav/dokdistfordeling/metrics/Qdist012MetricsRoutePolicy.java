package no.nav.dokdistfordeling.metrics;

import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistfordeling.metrics.MetricLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistfordeling.metrics.MetricLabels.TYPE_TECHNICAL_EXCEPTION;
import static no.nav.dokdistfordeling.qdist012.Qdist012Route.QDIST012_SERVICE_ID;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.ValidationException;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Component
public class Qdist012MetricsRoutePolicy extends RoutePolicySupport {

	private final MeterRegistry registry;
	private Timer.Sample timer;

	private static final String EXCEPTION_COUNTER = "dok_metric_exception_total";
	private static final String QDIST012_PROCESS_TIMER = "dok_route_latency_histogram";
	private static final String QDIST012_PROCESS_TIMER_DESCRIPTION = "prosesseringstid for kall inn til qdist012";
	private static final String QDIST012_START = "Qdist012_start";

	@Inject
	public Qdist012MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
		registry.counter(QDIST012_START).increment();
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		timer.stop(Timer.builder(QDIST012_PROCESS_TIMER)
				.description(QDIST012_PROCESS_TIMER_DESCRIPTION)
				.tags(LABEL_PROCESS, QDIST012_SERVICE_ID)
				.publishPercentileHistogram(true)
				.register(registry));

		if (exception != null) {
			if (isFunctionalException(exception)) {
				registry.counter(EXCEPTION_COUNTER,
						LABEL_ERROR_TYPE, TYPE_FUNCTIONAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						LABEL_PROCESS, QDIST012_SERVICE_ID).increment();
			} else {
				registry.counter(EXCEPTION_COUNTER,
						LABEL_ERROR_TYPE, TYPE_TECHNICAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						LABEL_PROCESS, QDIST012_SERVICE_ID).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof AbstractDokdistfordelingFunctionalException) || (e instanceof ValidationException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() instanceof Exception) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
