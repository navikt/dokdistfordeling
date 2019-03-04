package no.nav.dokdistfordeling.prometheus;

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

	private final String QDIST008_SLUTT = "Qdist008_slutt";
	private final String QDIST008_START = "Qdist008_start";
	private final String QDIST008_TECHNICAL = "Qdist008_technical_exception";
	private final String QDIST008_FUNCTIONAL = "Qdist008_functional_exception";

	private final MeterRegistry registry;
	private Timer.Sample timer;

	public Qdist008MetricsRoutePolicy(MeterRegistry registry) {
		this.registry = registry;
		registry.counter(QDIST008_START);
		registry.counter(QDIST008_SLUTT);
		registry.counter(QDIST008_TECHNICAL);
		registry.counter(QDIST008_FUNCTIONAL);
	}

	@Override
	public void onExchangeBegin(Route route, Exchange exchange) {
		timer = Timer.start(registry);
		registry.counter(QDIST008_START);
	}

	@Override
	public void onExchangeDone(Route route, Exchange exchange) {
		Exception exception = getException(exchange);

		if (exception == null) {
			timer.stop(Timer.builder(route.getId())
					.description(route.getDescription())
					.tags("process", "distribuerForsendelseMapper")
					.publishPercentileHistogram(true)
					.register(registry));
			registry.counter(QDIST008_SLUTT).increment();
		} else {
			if (isFunctionalException(exception)) {
				registry.counter(QDIST008_FUNCTIONAL).increment();
			} else {
				registry.counter(QDIST008_TECHNICAL).increment();
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
