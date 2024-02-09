package no.nav.dokdistfordeling.config.azure;

import no.nav.dokdistfordeling.config.props.DokdistfordelingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

@Configuration
public class OAuthEnabledWebClientConfig {
	public static final String CLIENT_REGISTRATION_DOKDISTADMIN = "azure-dokdistadmin";
	public static final String CLIENT_REGISTRATION_DOKARKIV = "azure-dokarkiv";
	public static final String CLIENT_REGISTRATION_DOKDISTKANAL = "azure-dokdistkanal";
	public static final String CLIENT_REGISTRATION_DOKMET = "azure-dokmet";
	public static final String CLIENT_REGISTRATION_PDL = "azure-pdl";

	@Bean
	WebClient webClient(ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, HttpClient nettyProxyHttpClient) {
		var oAuth2AuthorizedClientExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
		var clientHttpConnector = new ReactorClientHttpConnector(nettyProxyHttpClient);
		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.filter(oAuth2AuthorizedClientExchangeFilterFunction)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService,
			HttpClient nettyProxyHttpClient
	) {
		ClientCredentialsReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();


		WebClient webClientWithProxy = createReactiveProxyTokenWebClient(nettyProxyHttpClient);

		var client = new WebClientReactiveClientCredentialsTokenResponseClient();
		client.setWebClient(webClientWithProxy);

		authorizedClientProvider.setAccessTokenResponseClient(client);

		var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}


	/**
	 * @return WebClient med webproxy støtte
	 */

	private static WebClient createReactiveProxyTokenWebClient(HttpClient nettyProxyHttpClient) {
		var clientHttpConnector = new ReactorClientHttpConnector(nettyProxyHttpClient);
		return WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Bean
	ReactiveClientRegistrationRepository clientRegistrationRepository(List<ClientRegistration> clientRegistration) {
		return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
	}

	@Bean
	List<ClientRegistration> clientRegistration(AzureProperties azureProperties, DokdistfordelingProperties properties) {
		return List.of(ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_DOKDISTADMIN)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(properties.getEndpoints().getDokdistadmin().getScope())
						.build(),
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_DOKARKIV)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(properties.getEndpoints().getDokarkiv().getScope())
						.build(),
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_DOKDISTKANAL)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(properties.getEndpoints().getDokdistkanal().getScope())
						.build(),
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_DOKMET)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(properties.getEndpoints().getDokmet().getScope())
						.build(),
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_PDL)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(properties.getEndpoints().getPdl().getScope())
						.build());
	}


	/**
	 * @return Singleton netty HttpClient med støtte for webproxy
	 */

	@Bean
	HttpClient nettyProxyHttpClient() {
		return HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.ofSeconds(20));
	}
}
