package no.nav.dokdistfordeling.config.props;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("dokdistfordeling")
public class DokdistfordelingProperties {

	@Valid
	private final Endpoints endpoints = new Endpoints();
	@Valid
	private final Serviceuser serviceuser = new Serviceuser();

	@Data
	public static class Serviceuser {
		@NotEmpty
		private String username;

		@NotEmpty
		private String password;
	}

	@Data
	public static class Endpoints {
		/**
		 * URL til dokmet.
		 */
		@Valid
		@NotNull
		private Endpoint dokmet;

		/**
		 * URL til regoppslag.
		 */
		@Valid
		@NotNull
		private AzureEndpoint regoppslag;

		/**
		 * URL til saf.
		 */
		@Valid
		@NotNull
		private AzureEndpoint saf;

		/**
		 * URL til dokarkiv journalpost api.
		 */
		@Valid
		@NotNull
		private AzureEndpoint dokarkiv;

		/**
		 * URL til dokdistadmin administrerforsendelse api.
		 */
		@Valid
		@NotNull
		private AzureEndpoint dokdistadmin;

		/**
		 * URL og Scope til dokdistkanal.
		 */
		@Valid
		@NotNull
		private AzureEndpoint dokdistkanal;

		/**
		 * URL og Scope til PDL.
		 */
		@Valid
		@NotNull
		private AzureEndpoint pdl;

	}

	@Data
	public static class AzureEndpoint {
		/**
		 * Url til tjeneste som har azure autorisasjon
		 */
		@NotEmpty
		private String url;

		/**
		 * Scope til azure client credential flow
		 */
		@NotEmpty
		private String scope;
	}

	@Data
	public static class Endpoint {
		@NotEmpty
		private String url;
	}

}