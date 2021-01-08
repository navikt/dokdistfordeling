package no.nav.dokdistfordeling.itest.config;

import static org.mockito.Mockito.mock;

import com.amazonaws.services.s3.AmazonS3;
import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.config.alias.ArkiverDokumentproduksjonV1Alias;
import no.nav.dokdistfordeling.config.alias.MqGatewayAlias;
import no.nav.dokdistfordeling.config.alias.ServiceuserAlias;
import no.nav.dokdistfordeling.config.props.PdlProperties;
import no.nav.dokdistfordeling.config.props.SrvAppserverProperties;
import no.nav.dokdistfordeling.storage.S3Storage;
import no.nav.dokdistfordeling.storage.Storage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@EnableConfigurationProperties({
		ServiceuserAlias.class,
		ArkiverDokumentproduksjonV1Alias.class,
		MqGatewayAlias.class,
		SrvAppserverProperties.class,
		PdlProperties.class
})
@Import({
		Qdist008JmsItestConfig.class,
		LokalTestCacheConfig.class,
		STSTestConfig.class,
		CoreConfig.class})
public class Qdist008ItestConfig {

	@Bean
	public AmazonS3 s3() {
		return mock(AmazonS3.class);
	}

	@Bean
	public Storage storage(AmazonS3 s3) {
		return new S3Storage(s3);
	}

}

