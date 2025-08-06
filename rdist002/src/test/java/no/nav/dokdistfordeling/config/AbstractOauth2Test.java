package no.nav.dokdistfordeling.config;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static no.nav.dokdistfordeling.web.TokenClaimExtractor.ISSUER_ENTRA;

@EnableMockOAuth2Server
public abstract class AbstractOauth2Test {
	private static final String AZP_NAME = "dev-fss:teamdokumenthandtering:dokdistkanal";
	protected static final String OID = "4e5a62ad-a76d-4a67-8eac-8ff15b3b48fa";

	@Autowired
	public MockOAuth2Server mockOAuth2Server;

	public String jwt() {
		return jwt("app-client-id", Map.ofEntries(
				Map.entry("azp_name", AZP_NAME),
				Map.entry("oid", OID)));
	}

	public String jwt(String audience) {
		return jwt(audience, Map.ofEntries(
				Map.entry("azp_name", AZP_NAME),
				Map.entry("oid", OID)));
	}

	private String jwt(String audience, Map<String, String> claims) {
		return mockOAuth2Server.issueToken(
				ISSUER_ENTRA,
				"other-client-id",
				new DefaultOAuth2TokenCallback(
						ISSUER_ENTRA,
						"subject",
						"JWT",
						List.of(audience),
						claims,
						60
				)
		).serialize();
	}
}
