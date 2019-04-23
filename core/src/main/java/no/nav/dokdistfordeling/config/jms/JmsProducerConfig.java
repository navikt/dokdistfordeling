package no.nav.dokdistfordeling.config.jms;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.jms.Queue;

@Configuration
public class JmsProducerConfig {

	private Queue qdist012;

	public JmsProducerConfig(Queue qdist012) {
		this.qdist012 = qdist012;
	}

	@Bean
	public DistribuerForsendelseProducer qdist012Producer() {
		return new DistribuerForsendelseProducerImpl(qdist012);
	}

}
