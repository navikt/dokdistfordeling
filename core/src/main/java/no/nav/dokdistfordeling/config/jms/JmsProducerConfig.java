package no.nav.dokmot.config;

import static no.nav.dokmot.config.QueueConfig.BESTEM_VIDEREBEHANLDING_QUEUE_NAME;

import no.nav.dokmot.jms.producer.Producer;
import no.nav.dokmot.jms.producer.internal.DefaultInternalProducer;
import no.nav.dokmot.jms.producer.support.ViderebehandlingMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.Queue;

/**
 * JMS producer config for Viderebehandling queues between QMOT001 and QMOT100
 *
 * @author Leo-Andreas Ervik, Visma Consulting
 */
@Configuration
@Import(JmsConfig.class)
public class JmsProducerConfig {

	public static final String QMOT100_VIDEREBEHANDLING_PRODUCER = "qmot100Producer";

	@Inject
	@Named(BESTEM_VIDEREBEHANLDING_QUEUE_NAME)
	private Queue bestemVidereBehandlingQueue;

	@Bean
	public ViderebehandlingMapper viderebehandlingMapper() {
		return new ViderebehandlingMapper();
	}

	@Bean(name = QMOT100_VIDEREBEHANDLING_PRODUCER)
	public Producer qmot100Producer() {
		return new DefaultInternalProducer(bestemVidereBehandlingQueue);
	}

}
