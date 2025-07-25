package no.nav.dokdistfordeling.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.azure.AzureProperties;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.Constants.HEADER_NAV_CALLID;
import static no.nav.dokdistfordeling.constants.Constants.USER_ID;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.UKJENT_CONSUMER_ID;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.UKJENT_USER_ID;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.getConsumerId;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.getUserId;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class MDCHandlerInterceptor implements HandlerInterceptor {
	private final TokenValidationContextHolder tokenValidationContextHolder;
	private final AzureProperties azureProperties;

	public MDCHandlerInterceptor(TokenValidationContextHolder tokenValidationContextHolder,
								 AzureProperties azureProperties) {
		this.tokenValidationContextHolder = tokenValidationContextHolder;
		this.azureProperties = azureProperties;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		TokenValidationContext tokenValidationContext = tokenValidationContextHolder.getTokenValidationContext();

		JwtToken jwtToken = tokenValidationContext.getFirstValidToken();
		if (jwtToken == null) {
			return true;
		}

		populateCallId(request);
		populateConsumerId(tokenValidationContext, jwtToken);
		populateUserId(tokenValidationContext, jwtToken);
		markUsingSafClientId(jwtToken);
		return true;
	}

	private void populateCallId(HttpServletRequest request) {
		final String navCallId = request.getHeader(HEADER_NAV_CALLID);

		if (isNotBlank(navCallId)) {
			MDC.put(CALL_ID, navCallId);
			return;
		}

		MDC.put(CALL_ID, UUID.randomUUID().toString());
	}

	private void populateConsumerId(TokenValidationContext tokenValidationContext, JwtToken jwtToken) {
		final String consumerId = getConsumerId(tokenValidationContext, jwtToken);

		if (isNotBlank(consumerId)) {
			MDC.put(CONSUMER_ID, consumerId);
			return;
		}

		MDC.put(CONSUMER_ID, UKJENT_CONSUMER_ID);
	}

	private void populateUserId(TokenValidationContext tokenValidationContext, JwtToken jwtToken) {
		final String userId = getUserId(tokenValidationContext, jwtToken);

		if (isNotBlank(userId)) {
			MDC.put(USER_ID, userId);
			return;
		}

		MDC.put(USER_ID, UKJENT_USER_ID);
	}

	/**
	 * @deprecated Fjernes etter at alle klienter har gått over til å hente token
	 * med dokdistfordeling-scope i stedet for saf-scope
	 */
	@Deprecated
	private void markUsingSafClientId(JwtToken jwtToken) {
		try {
			List<String> aud = jwtToken.getJwtTokenClaims().getAsList("aud");
			if (aud.contains(azureProperties.appClientId())) {
				MDC.put("saf_clientid", "false");
			} else {
				MDC.put("saf_clientid", "true");
			}
		} catch (Exception e) {
			// noop
		}
	}
}
