package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.azure.AzureConfig;
import no.nav.dokdistfordeling.config.dokarkiv.JournalpostApiConfig;
import no.nav.dokdistfordeling.config.props.DokdistmellomlagerProperties;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.security.AzureToken;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

import static java.lang.System.getenv;
import static java.lang.System.setProperty;

@EnableCaching
@EnableRetry
@SpringBootApplication
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		MqGatewayAlias.class,
		PdlProperties.class,
		AzureConfig.class,
		JournalpostApiConfig.class,
		DokdistmellomlagerProperties.class
})
@Import({
		CoreConfig.class,
		DistribuerJournalpostConfig.class,
		AzureToken.class
})
public class Application {

	public static void main(String[] args) {
		setProperty("javax.net.ssl.keyStorePassword", getenv("DOKDISTFORDELING_CERT_KEYSTORE_PASSWORD"));
		SpringApplication.run(Application.class, args);
	}

}

