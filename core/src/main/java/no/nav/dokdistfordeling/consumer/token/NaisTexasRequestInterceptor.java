package no.nav.dokdistfordeling.consumer.token;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.consumer.NavHeaders.NAV_CALL_ID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class NaisTexasRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String TARGET_SCOPE = "targetScope";
	private final NaisTexasConsumer naisTexasConsumer;

	public NaisTexasRequestInterceptor(NaisTexasConsumer naisTexasConsumer) {
		this.naisTexasConsumer = naisTexasConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		Map<String, Object> attributes = request.getAttributes();
		if (!attributes.containsKey(TARGET_SCOPE)) {
			throw new IllegalArgumentException("Kan ikke bruke denne restClient uten at targetScope attributtet er satt");
		}
		String targetScope = (String) attributes.get(TARGET_SCOPE);
		request.getHeaders().setBearerAuth(naisTexasConsumer.getSystemToken(targetScope));
		request.getHeaders().add(NAV_CALL_ID, getMDCCallId());
		return execution.execute(request, body);
	}

	private static String getMDCCallId() {
		String callId = MDC.get(CALL_ID);
		return isNotBlank(callId) ? callId : UUID.randomUUID().toString();
	}
}
