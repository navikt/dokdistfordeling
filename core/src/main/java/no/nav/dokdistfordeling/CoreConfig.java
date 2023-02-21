package no.nav.dokdistfordeling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static java.time.Duration.ofSeconds;

@ComponentScan
@Configuration
public class CoreConfig {
	@Bean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		HttpClient httpClient = HttpClient.create().responseTimeout(ofSeconds(20))
				.proxyWithSystemProperties();
		return webClientBuilder
				.clone()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
