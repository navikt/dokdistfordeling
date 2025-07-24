package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.SecurityConfig;
import no.nav.dokdistfordeling.config.props.MqProperties;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import no.nav.dokdistfordeling.config.props.DokdistmellomlagerProperties;
import no.nav.dokdistfordeling.config.props.NaisProperties;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@EnableCaching
@EnableRetry
@EnableConfigurationProperties({
		MqProperties.class,
		AzureProperties.class,
		DokdistmellomlagerProperties.class,
		DokdistfordelingProperties.class,
		NaisProperties.class
})
@Import({
		CoreConfig.class,
		DistribuerJournalpostConfig.class,
		SecurityConfig.class
})
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}

