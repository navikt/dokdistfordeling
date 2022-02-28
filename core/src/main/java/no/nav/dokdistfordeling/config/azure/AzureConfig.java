package no.nav.dokdistfordeling.config.azure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Data
@ConfigurationProperties("azure.app")
@Validated
public class AzureConfig {

    @NotEmpty
    private String tokenUrl;
    @NotEmpty
    private String scope;
    @NotEmpty
    private String clientId;
    @NotEmpty
    private String clientSecret;

}
