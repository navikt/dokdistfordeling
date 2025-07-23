package no.nav.dokdistfordeling.web;

import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.jwt.JwtTokenClaims;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Henter ut claims fra rest-sts / azure token for sporing.
 * Ingen tilstand / trådsikker.
 */
public final class TokenClaimExtractor {
	// Azure claims. https://docs.microsoft.com/en-us/azure/active-directory/develop/access-tokens#payload-claims
	static final String AZURE_CLAIM_AZP = "azp";
	static final String AZURE_CLAIM_OID = "oid";
	static final String AZURE_CLAIM_SUB = "sub";
	// NAV custom Azure claim. https://doc.nais.io/security/auth/azure-ad/configuration/#extra
	static final String AZURE_NAV_CUSTOM_CLAIM_NAVIDENT = "NAVident";
	static final String AZURE_NAV_CUSTOM_CLAIM_AZP_NAME = "azp_name";
	static final String UKJENT_CONSUMER_ID = "ukjentConsumer";
	static final String UKJENT_USER_ID = "ukjentBruker";
	private static final String SERVICEUSER_PREFIX = "srv";

	private TokenClaimExtractor() {
		// ingen instansiering
	}

	public static String getConsumerId(JwtToken jwtToken) {
		if (isRestStsSystemToken(jwtToken)) {
			return jwtToken.getSubject();
		} else if (isClientCredentialFlowToken(jwtToken) || isOnBehalfOfFlowToken(jwtToken)) {
			return findAzureAppnameClaim(jwtToken.getJwtTokenClaims());
		}
		return UKJENT_CONSUMER_ID;
	}

	public static String getUserId(JwtToken jwtToken) {
		if (isRestStsSystemToken(jwtToken)) {
			return jwtToken.getSubject();
		} else if (isClientCredentialFlowToken(jwtToken)) {
			return findAzureAppnameClaim(jwtToken.getJwtTokenClaims());
		} else if (isOnBehalfOfFlowToken(jwtToken)) {
			if (jwtToken.getJwtTokenClaims().getAllClaims().containsKey(AZURE_NAV_CUSTOM_CLAIM_NAVIDENT)) {
				return jwtToken.getJwtTokenClaims().getStringClaim(AZURE_NAV_CUSTOM_CLAIM_NAVIDENT);
			} else {
				return jwtToken.getJwtTokenClaims().getStringClaim(AZURE_CLAIM_OID);
			}
		}
		return UKJENT_USER_ID;
	}

	public static boolean isRestStsSystemToken(JwtToken jwtToken) {
		return jwtToken.getSubject().toLowerCase().startsWith(SERVICEUSER_PREFIX);
	}

	public static boolean isOnBehalfOfFlowToken(JwtToken jwtToken) {
		final JwtTokenClaims jwtTokenClaims = jwtToken.getJwtTokenClaims();
		return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) != null &&
			   jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID) != null &&
			   !jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB).equals(jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID));
	}

	private static boolean isClientCredentialFlowToken(JwtToken jwtToken) {
		final JwtTokenClaims jwtTokenClaims = jwtToken.getJwtTokenClaims();
		return jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB) != null &&
			   jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID) != null &&
			   jwtTokenClaims.getStringClaim(AZURE_CLAIM_SUB).equals(jwtTokenClaims.getStringClaim(AZURE_CLAIM_OID));
	}

	private static String findAzureAppnameClaim(JwtTokenClaims jwtTokenClaims) {
		if (jwtTokenClaims.getAllClaims().containsKey(AZURE_NAV_CUSTOM_CLAIM_AZP_NAME)) {
			String azpnameClaim = jwtTokenClaims.getStringClaim(AZURE_NAV_CUSTOM_CLAIM_AZP_NAME);
			if (isNotBlank(azpnameClaim)) {
				return azpnameClaim;
			}
			return jwtTokenClaims.getStringClaim(AZURE_CLAIM_AZP);
		}
		return jwtTokenClaims.getStringClaim(AZURE_CLAIM_AZP);
	}
}
