package no.nav.dokdistfordeling.config.azure;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.validation.constraints.NotEmpty;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Data
@ConfigurationProperties("azure")
@Validated
public class AzureConfig {

    @NotEmpty
    private String openidConfigTokenEndpoint;
    @NotEmpty
    private String appScope;
    @NotEmpty
    private String appClientId;
    @NotEmpty
    private String appClientSecret;

    @Bean("azureClient")
    public WebClient webClient(WebClient.Builder webClientBuilder,
                               @Value("${proxy.host:#{null}}") String proxyHost) {

        HttpClient httpClient = HttpClient.create();

        if (!isBlank(proxyHost)) {
            var proxyUri = URI.create(proxyHost);
            httpClient = httpClient
                    .proxy(proxy -> proxy
                            .type(ProxyProvider.Proxy.HTTP)
                            .host(proxyUri.getHost())
                            .port(proxyUri.getPort()));
        }

        return webClientBuilder
                .clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(openidConfigTokenEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

}
