package no.nav.dokdistfordeling.config.jms;

import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.dokdistfordeling.qdist012.ObjectFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.converter.SimpleMessageConverter;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.Queue;

public class HentDokumenterFraJoarkProducerImpl implements HentDokumenterFraJoarkProducer {

	@Inject
	private JmsTemplate jmsTemplate;

	private Queue queue;

	private ObjectFactory objectFactory = new ObjectFactory();

	public HentDokumenterFraJoarkProducerImpl(Queue queue) {
		this.queue = queue;
	}

	@Override
	public void produce(HentDokumenterFraJoark hentDokumenterFraJoark) {
		jmsTemplate.convertAndSend(
				queue,
				objectFactory.createHentDokumenterFraJoark(hentDokumenterFraJoark),
				message -> (Message) new SimpleMessageConverter().toMessage(message, null));

	}
}
