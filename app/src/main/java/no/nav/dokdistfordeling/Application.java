package no.nav.dokdistfordeling;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import no.nav.dokdistfordeling.prometheus.Qdist008MetricsRoutePolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableRetry
@EnableConfigurationProperties({ServiceuserAlias.class,
		ArkiverDokumentproduksjonV1Alias.class,
		MqGatewayAlias.class,
		SrvAppserverProperties.class})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	Qdist008MetricsRoutePolicy createRoutePolicy(MeterRegistry meterRegistry) {
		return new Qdist008MetricsRoutePolicy(meterRegistry);
	}
}

