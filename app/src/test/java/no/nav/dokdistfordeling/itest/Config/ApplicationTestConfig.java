package no.nav.dokdistfordeling.itest.Config;

import static org.mockito.Mockito.mock;

import no.nav.dokdistfordeling.storage.Storage;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
@Profile("itest")
@Import(JmsItestConfig.class)
public class ApplicationTestConfig {

	@Bean
	public Storage storage() {
		return mock(Storage.class);
	}
}

