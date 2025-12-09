package no.nav.dokdistfordeling.dokdistdb.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("dokdistadmin")
public record DokdistadmindbProperties(
		@Valid
		Database database) {

	public record Database(
			@Positive
			int poolsize,
			@NotEmpty
			String schema,
			String onshosts) {

		public Database {
			poolsize = 10;
		}
	}

}
