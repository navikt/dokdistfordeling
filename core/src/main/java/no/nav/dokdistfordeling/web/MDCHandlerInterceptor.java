package no.nav.dokdistfordeling.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.Constants.HEADER_NAV_CALLID;
import static no.nav.dokdistfordeling.constants.Constants.USER_ID;
import static no.nav.dokdistfordeling.util.MappingUtil.splitBearerToken;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.UKJENT_CONSUMER_ID;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.UKJENT_USER_ID;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.getConsumerId;
import static no.nav.dokdistfordeling.web.TokenClaimExtractor.getUserId;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class MDCHandlerInterceptor implements HandlerInterceptor {
	private final TokenValidationContextHolder tokenValidationContextHolder;

	public MDCHandlerInterceptor(TokenValidationContextHolder tokenValidationContextHolder) {
		this.tokenValidationContextHolder = tokenValidationContextHolder;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		TokenValidationContext tokenValidationContext = tokenValidationContextHolder.getTokenValidationContext();

		JwtToken jwtToken = extractToken(request, tokenValidationContext);
		if (jwtToken == null) {
			return true;
		}

		populateCallId(request);
		populateConsumerId(jwtToken);
		populateUserId(jwtToken);
		return true;
	}

	@Nullable
	private static JwtToken extractToken(HttpServletRequest request, TokenValidationContext tokenValidationContext) {
		JwtToken firstValidToken = tokenValidationContext.getFirstValidToken();
		if (firstValidToken != null) {
			return firstValidToken;
		}
		String authorizationHeader = request.getHeader(AUTHORIZATION);
		if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
			return new JwtToken(splitBearerToken(authorizationHeader));
		}
		return null;
	}

	private void populateCallId(HttpServletRequest request) {
		final String navCallId = request.getHeader(HEADER_NAV_CALLID);

		if (isNotBlank(navCallId)) {
			MDC.put(CALL_ID, navCallId);
			return;
		}

		MDC.put(CALL_ID, UUID.randomUUID().toString());
	}

	private void populateConsumerId(JwtToken jwtToken) {
		final String consumerId = getConsumerId(jwtToken);

		if (isNotBlank(consumerId)) {
			MDC.put(CONSUMER_ID, consumerId);
			return;
		}

		MDC.put(CONSUMER_ID, UKJENT_CONSUMER_ID);
	}

	private void populateUserId(JwtToken jwtToken) {
		final String userId = getUserId(jwtToken);

		if (isNotBlank(userId)) {
			MDC.put(USER_ID, userId);
			return;
		}

		MDC.put(USER_ID, UKJENT_USER_ID);
	}
}
