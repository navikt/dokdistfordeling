package no.nav.dokdistfordeling.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.DistribuerJournalpostConfig;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.config.dokarkiv.JournalpostApiConfig;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.security.AzureToken;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		PdlProperties.class,
		AzureConfig.class,
		JournalpostApiConfig.class
})
@EnableRetry
@Import({
		Rdist002JmsItestConfig.class,
		DistribuerJournalpostConfig.class,
		CoreConfig.class,
		AzureToken.class,
		JournalpostApi.class
})
public class Rdist002TestConfig {

}

