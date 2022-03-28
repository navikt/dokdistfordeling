package no.nav.dokdistfordeling.springdoc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Configuration
public class SpringdocConfig {

	@Bean
	public OpenAPI dokdistfordelingRestApi(@Value("${NAIS_APP_IMAGE:1-SNAPSHOT}") String version) {
		return new OpenAPI()
				.info(new Info()
						.title("DistribuerJournalpost API")
						.description("""
								Her dokumenteres tjenestegrensesnittet til distribuerJournalpost. Til autentisering brukes OIDC-token (JWT via OAuth2.0).\s
								Følgende format må brukes i Authorize sitt input-felt "Value": <strong> Bearer {token} </strong>.\s
								Eksempel på verdi i input-feltet: <strong> Bearer eYdmifml0ejugm </strong>. Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet.
								""")
						.version(version))
				.components(
						new Components()
								.addSecuritySchemes("Authorization",
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("Bearer")
												.bearerFormat("JWT")
												.in(SecurityScheme.In.HEADER)
												.description("Eksempel på verdi som skal inn i Value-feltet (Bearer trengs altså ikke å oppgis): 'eyAidH...'")
												.name(HttpHeaders.AUTHORIZATION)
								)
				)
				.addSecurityItem(
						new SecurityRequirement()
								.addList("Authorization")
				);
	}
}
