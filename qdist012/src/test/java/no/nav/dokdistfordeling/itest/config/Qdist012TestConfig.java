package no.nav.dokdistfordeling.itest.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.props.MqProperties;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.config.props.NaisProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.storage.BucketStorage;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		DokdistfordelingProperties.class,
		MqProperties.class,
		AzureProperties.class,
		NaisProperties.class
})
@Import({
		Qdist012JmsItestConfig.class,
		CoreConfig.class,
		JournalpostApi.class,
		AbstractRepositoryTest.class
})
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
public class Qdist012TestConfig {

	@Bean
	public BucketStorage bucketStorage() {
		return mock(BucketStorage.class);
	}
}

