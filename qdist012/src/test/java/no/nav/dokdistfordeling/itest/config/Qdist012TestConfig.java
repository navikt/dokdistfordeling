package no.nav.dokdistfordeling.itest.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.config.dokarkiv.JournalpostApiConfig;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.storage.BucketStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import static org.mockito.Mockito.mock;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		PdlProperties.class,
		AzureConfig.class,
		JournalpostApiConfig.class
})
@Import({
		Qdist012JmsItestConfig.class,
		CoreConfig.class,
		AzureToken.class,
		JournalpostApi.class
})
public class Qdist012TestConfig {

	@Bean
	public BucketStorage bucketStorage() {
		return mock(BucketStorage.class);
	}
}

