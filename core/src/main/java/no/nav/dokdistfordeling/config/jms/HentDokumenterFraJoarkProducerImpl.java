package no.nav.dokdistfordeling.config.jms;

import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.dokdistfordeling.qdist012.ObjectFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.converter.SimpleMessageConverter;

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class HentDokumenterFraJoarkInternalProducer implements HentDokumenterFraJoarkProducer {

	@Inject
	private JmsTemplate jmsTemplate;

	private Queue queue;

	private ObjectFactory objectFactory = new ObjectFactory();

	private Marshaller marshaller = JAXBContext.newInstance(HentDokumenterFraJoark.class).createMarshaller();

	public HentDokumenterFraJoarkInternalProducer(Queue queue) throws JAXBException {
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
