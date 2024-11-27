package no.nav.dokdistfordeling.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties("dokdistfordeling")
public class DokdistfordelingProperties {

	private final Endpoints endpoints = new Endpoints();
	private final Serviceuser serviceuser = new Serviceuser();

	@Data
	@Validated
	public static class Serviceuser {
		@NotEmpty
		private String username;
		@NotEmpty
		private String password;
	}

	@Data
	@Validated
	public static class Endpoints {
		/**
		 * URL til dokmet.
		 */
		@NotNull
		private Endpoint dokmet;

		/**
		 * URL til dokarkiv journalpost api.
		 */
		@NotNull
		private AzureEndpoint dokarkiv;

		/**
		 * URL til dokdistadmin administrerforsendelse api.
		 */
		@NotNull
		private AzureEndpoint dokdistadmin;

		/**
		 * URL og Scope til dokdistkanal.
		 */
		@NotNull
		private AzureEndpoint dokdistkanal;

		/**
		 * URL og Scope til PDL.
		 */
		@NotNull
		private AzureEndpoint pdl;

	}

	@Data
	@Validated
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
	@Validated
	public static class Endpoint {
		@NotEmpty
		private String url;
	}

}