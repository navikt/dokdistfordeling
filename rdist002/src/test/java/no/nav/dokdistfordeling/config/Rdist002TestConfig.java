package no.nav.dokdistfordeling.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.DistribuerJournalpostConfig;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@Profile("itest")
@EnableConfigurationProperties({
		DokdistfordelingProperties.class,
		MqGatewayAlias.class,
		AzureProperties.class
})
@EnableRetry
@Import({
		Rdist002JmsItestConfig.class,
		DistribuerJournalpostConfig.class,
		CoreConfig.class,
		JournalpostApi.class,
		SecurityConfig.class
})
public class Rdist002TestConfig {
}



