package no.nav.dokmot.jms.producer.internal;

import static no.nav.dokmot.domain.MDCConstants.MDC_APP_ID;
import static no.nav.dokmot.prometheus.PrometheusLabels.EVENT_SENT_TO_QMOT100;
import static no.nav.dokmot.prometheus.PrometheusMetrics.getProcessName;
import static no.nav.dokmot.prometheus.PrometheusMetrics.requestCounter;

import no.nav.dokmot.jms.MDCMessageConverter;
import no.nav.dokmot.jms.producer.Producer;
import no.nav.dokmot.jms.producer.support.ViderebehandlingMapper;
import no.nav.dokmot.jms.producer.support.ViderebehandlingTo;
import no.nav.dokmot.viderebehandling.xml.jaxb2.gen.ObjectFactory;
import no.nav.modig.common.MDCOperations;
import org.springframework.jms.core.JmsTemplate;

import javax.inject.Inject;
import javax.jms.Queue;

/**
 * Viderebehandling JMS queuer
 *
 * @author Leo-Andreas Ervik, Visma Consulting
 */
public class DefaultInternalProducer implements Producer {
	
	@Inject
	private ViderebehandlingMapper mapper;
	@Inject
	private JmsTemplate jmsTemplate;
	
	private Queue queue;
	private ObjectFactory objectFactory = new ObjectFactory();
	
	public DefaultInternalProducer(Queue queue) {
		this.queue = queue;
	}
	
	@Override
	
	public void produce(ViderebehandlingTo to) {
		jmsTemplate.convertAndSend(
				queue,
				objectFactory.createViderebehandling(mapper.map(to)),
				message -> new MDCMessageConverter().toMessage(message, null));
		
		requestCounter.labels(getProcessName(MDCOperations.getFromMDC(MDC_APP_ID)), EVENT_SENT_TO_QMOT100).inc();
	}
}
