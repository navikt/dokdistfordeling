package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.config.dokarkiv.JournalpostApiConfig;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import no.nav.dokdistfordeling.security.AzureToken;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@EnableCaching
@EnableRetry
@SpringBootApplication
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
		CoreConfig.class,
		DistribuerJournalpostConfig.class,
		AzureToken.class
})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

