package no.nav.dokdistfordeling.config.props;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Getter
@Setter
@ToString
@ConfigurationProperties("mqgateway01")
@Validated
public class MqProperties {
	@NotEmpty
	private String hostname;
	@NotEmpty
	private String name;
	@Min(0)
	private int port;
	@Valid
	private MqChannel channel = new MqChannel();

	@Data
	public static class MqChannel {
		@NotBlank
		private String securename;
	}
}
