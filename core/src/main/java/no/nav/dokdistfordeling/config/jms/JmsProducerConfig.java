package no.nav.dokdistfordeling.config.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;

@Configuration
public class JmsProducerConfig {

	private JmsTemplate jmsTemplate;
	private Queue qdist012;

	@Value("${hentdokumenter_fra_joark_crypto_password}")
	private String encryptionPassphrase;

	public JmsProducerConfig(JmsTemplate jmsTemplate,
							 Queue qdist012) {
		this.jmsTemplate = jmsTemplate;
		this.qdist012 = qdist012;
	}

	@Bean
	public DistribuerForsendelseProducer qdist012Producer() {
		return new DistribuerForsendelseProducerImpl(jmsTemplate, qdist012, encryptionPassphrase);
	}

}
