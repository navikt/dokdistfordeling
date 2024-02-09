package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.SecurityConfig;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.config.props.DokdistmellomlagerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

import static java.lang.System.getenv;
import static java.lang.System.setProperty;

@EnableCaching
@EnableRetry
@EnableConfigurationProperties({
		MqGatewayAlias.class,
		AzureProperties.class,
		DokdistmellomlagerProperties.class,
		DokdistfordelingProperties.class
})
@Import({
		CoreConfig.class,
		DistribuerJournalpostConfig.class,
		SecurityConfig.class
})
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Application {

	public static void main(String[] args) {
		setProperty("javax.net.ssl.keyStorePassword", getenv("DOKDISTFORDELING_CERT_KEYSTORE_PASSWORD"));
		SpringApplication.run(Application.class, args);
	}

}

