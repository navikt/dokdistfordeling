package no.nav.dokdistfordeling.config.azure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties("proxy")
@Validated
public class ProxyConfig {

	private String host;
	private int port;
}
