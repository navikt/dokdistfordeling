package no.nav.dokdistfordeling.producer;

import org.apache.camel.Producer;
import org.springframework.jms.core.JmsTemplate;

import javax.inject.Inject;
import javax.jms.Queue;

public class DefaultInternalProducer  {
//public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {
//
//	@Inject
//	private ViderebehandlingMapper mapper;
//	@Inject
//	private JmsTemplate jmsTemplate;
//
//	private Queue queue;
//	private ObjectFactory objectFactory = new ObjectFactory();
//
//	public DistribuerForsendelseProducerImpl(Queue queue) {
//		this.queue = queue;
//	}
//
//	@Override
//	public void produce(ViderebehandlingTo to) {
//		jmsTemplate.convertAndSend(
//				queue,
//				objectFactory.createViderebehandling(mapper.map(to)),
//				message -> new MDCMessageConverter().toMessage(message, null));
//
//	}
}
