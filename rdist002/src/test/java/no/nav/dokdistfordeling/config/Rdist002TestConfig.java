package no.nav.dokdistfordeling.config;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.DistribuerJournalpostConfig;
import no.nav.dokdistfordeling.config.props.PdlProperties;
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
		ArkiverDokumentproduksjonV1Alias.class,
		MqGatewayAlias.class,
		PdlProperties.class
})
@EnableRetry
@Import({
		Rdist002JmsItestConfig.class,
		DistribuerJournalpostConfig.class,
		CoreConfig.class})
public class Rdist002TestConfig {

}

