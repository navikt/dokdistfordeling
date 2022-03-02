package no.nav.dokdistfordeling.itest.config;

import static org.mockito.Mockito.mock;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.config.dokarkiv.JournalpostApiConfig;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.security.AzureToken;
import no.nav.dokdistfordeling.storage.Storage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@EnableRetry
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		ArkiverDokumentproduksjonV1Alias.class,
		MqGatewayAlias.class,
		SrvAppserverProperties.class,
		PdlProperties.class,
		AzureConfig.class,
		JournalpostApiConfig.class
})
@Import({
		Qdist012JmsItestConfig.class,
		STSTestConfig.class,
		CoreConfig.class,
		AzureToken.class,
		JournalpostApi.class
})
public class Qdist012TestConfig {

	@Bean
	public Storage awsStorage() {
		return mock(Storage.class);
	}
}

