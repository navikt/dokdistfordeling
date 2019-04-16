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
	public HentDokumenterFraJoarkProducer qdist012Producer() {
		return new HentDokumenterFraJoarkProducerImpl(qdist012);
	}

}
