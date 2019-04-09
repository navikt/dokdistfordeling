package no.nav.dokdistfordeling.itest.config;

import static org.mockito.Mockito.mock;

import no.nav.dokdistfordeling.CoreConfig;
import no.nav.dokdistfordeling.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@Import({
		JmsItestConfig.class,
		LokalTestCacheConfig.class,
		STSTestConfig.class,
		CoreConfig.class})
public class ApplicationTestConfig {

	@Bean
	public Storage storage() {
		return mock(Storage.class);
	}
}

