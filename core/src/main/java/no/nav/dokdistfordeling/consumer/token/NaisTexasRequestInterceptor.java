package no.nav.dokdistfordeling.consumer.token;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

public class NaisTexasRequestInterceptor implements ClientHttpRequestInterceptor {

	public static final String TARGET_SCOPE = "targetScope";
	private final NaisTexasConsumer naisTexasConsumer;

	public NaisTexasRequestInterceptor(NaisTexasConsumer naisTexasConsumer) {
		this.naisTexasConsumer = naisTexasConsumer;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		Map<String, Object> attributes = request.getAttributes();
		if(!attributes.containsKey(TARGET_SCOPE)) {
			throw new IllegalArgumentException("Kan ikke bruke denne restClient uten at targetScope attributtet er satt");
		}
		String targetScope = (String) attributes.get(TARGET_SCOPE);
		request.getHeaders().setBearerAuth(naisTexasConsumer.getSystemToken(targetScope));
		return execution.execute(request, body);
	}
}
