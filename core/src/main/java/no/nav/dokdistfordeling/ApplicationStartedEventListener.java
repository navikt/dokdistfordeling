package no.nav.dokdistfordeling;

import io.micrometer.context.ContextRegistry;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import reactor.core.publisher.Hooks;

import static no.nav.dokdistfordeling.constants.Constants.ALL_MDC_KEYS;

public class ApplicationStartedEventListener implements ApplicationListener<ApplicationStartedEvent> {

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		registerReactorContextPropagation();
	}

	private static void registerReactorContextPropagation() {
		Hooks.enableAutomaticContextPropagation();
		ALL_MDC_KEYS.forEach(ApplicationStartedEventListener::registerMDCKey);
	}

	private static void registerMDCKey(String key) {
		ContextRegistry.getInstance().registerThreadLocalAccessor(
				key,
				() -> MDC.get(key),
				value -> MDC.put(key, value),
				() -> MDC.remove(key));
	}
}
