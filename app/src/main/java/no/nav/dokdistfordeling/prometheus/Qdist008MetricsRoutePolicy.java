package no.nav.dokdistfordeling.prometheus;

import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.LABEL_PROCESS;
import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.TYPE_TECHNICAL_EXCEPTION;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.ValidationException;
import org.apache.camel.support.RoutePolicySupport;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
public class Qdist008MetricsRoutePolicy extends RoutePolicySupport {

	public static final String QDIST008 = "Qdist008";
	private final String QDIST008_START = "Qdist008_start";
	private final String QDIST008_END = "Qdist008_end";
	private final String QDIST008_EXCEPTION = "request_exception_total";

	private final MeterRegistry registry;
	private Timer.Sample timer;

	public Qdist008MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
		registry.counter(QDIST008_START).increment();
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		if (exception == null) {
			timer.stop(Timer.builder(route.getId())
					.description(route.getDescription())
					.tags(LABEL_PROCESS, QDIST008)
					.publishPercentileHistogram(true)
					.register(registry));
			registry.counter(QDIST008_END,
					LABEL_PROCESS, QDIST008
			).increment();
		} else {
			if (isFunctionalException(exception)) {
				registry.counter(QDIST008_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_FUNCTIONAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getSimpleName(),
						LABEL_PROCESS, QDIST008).increment();
			} else {
				registry.counter(QDIST008_EXCEPTION,
						LABEL_ERROR_TYPE, TYPE_TECHNICAL_EXCEPTION,
						LABEL_EXCEPTION_NAME, exception.getClass().getCanonicalName(),
						LABEL_PROCESS, QDIST008).increment();
			}
		}
	}

	private boolean isFunctionalException(Exception e) {
		return (e instanceof DokdistfordelingFunctionalException) || (e instanceof ValidationException);
	}

	private Exception getException(Exchange exchange) {
		Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (exception == null && exchange.getException() instanceof Exception) {
			exception = (Exception) exchange.getException().getCause();
		}
		return exception;
	}
}
