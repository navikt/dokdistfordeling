package no.nav.dokdistfordeling.config.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.metrics.DokMonitoringAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("nais")
@Configuration
public class MetricConfig {

	@Bean
	DokMonitoringAspect timedAspect(MeterRegistry meterRegistry) {
		return new DokMonitoringAspect(meterRegistry);
	}

}
