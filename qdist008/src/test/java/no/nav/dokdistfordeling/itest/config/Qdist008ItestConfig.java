package no.nav.dokdistfordeling.itest.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.storage.BucketStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("itest")
@EnableConfigurationProperties({
		DokdistfordelingProperties.class,
		MqGatewayAlias.class,
		PdlProperties.class,
		AzureProperties.class
})
@Import({
		Qdist008JmsItestConfig.class,
		LokalTestCacheConfig.class,
		CoreConfig.class,
		JournalpostApi.class
})
public class Qdist008ItestConfig {

	@Bean
	public BucketStorage bucketStorage() {
		return mock(BucketStorage.class);
	}
}

