package no.nav.dokdistfordeling.config;

import lombok.extern.slf4j.Slf4j;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import jakarta.jms.ConnectionFactory;

/**
 * Rydder opp ressurser som Spring ikke gjør selv.
 */
@Slf4j
@Component
public class ShutdownHook {

	private final ConnectionFactory connectionFactory;

	public ShutdownHook(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	@PreDestroy
	public void destroy() {
		log.info("Graceful shutdown - Lukker koblinger til ConnectionFactory pool");
		((JmsPoolConnectionFactory) connectionFactory).clear();
	}
}
