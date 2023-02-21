package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
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
		MqGatewayAlias.class,
		PdlProperties.class,
		AzureProperties.class,
		DokdistmellomlagerProperties.class,
		DokdistfordelingProperties.class
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

