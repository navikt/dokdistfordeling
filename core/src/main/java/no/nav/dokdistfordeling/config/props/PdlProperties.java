package no.nav.dokdistfordeling.config.props;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@ConfigurationProperties("pdl")
@Validated
public class PdlProperties {
    @NotEmpty
    private String url;
}