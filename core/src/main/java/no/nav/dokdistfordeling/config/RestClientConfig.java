package no.nav.dokdistfordeling.config;

import no.nav.dokdistfordeling.consumer.token.NaisTexasConsumer;
import no.nav.dokdistfordeling.consumer.token.NaisTexasRequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient restClientTexas(RestClient.Builder restClientBuilder, NaisTexasConsumer naisTexasConsumer) {
		return restClientBuilder
				.defaultHeaders(httpHeaders -> httpHeaders.set(NAV_CALL_ID, MDC.get(CALL_ID)))
				.requestInterceptor(new NaisTexasRequestInterceptor(naisTexasConsumer))
				.build();
	}
}
