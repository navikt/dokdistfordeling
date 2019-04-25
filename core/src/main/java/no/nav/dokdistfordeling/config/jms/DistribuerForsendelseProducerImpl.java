package no.nav.dokdistfordeling.config.jms;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.exception.technical.MarshallingHentDokumenterFraJoarkTechnicalException;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.springframework.jms.core.JmsTemplate;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

@Slf4j
public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {

	@Inject
	private JmsTemplate jmsTemplate;

	private String encryptionPassphrase;
	private Queue queue;

	public DistribuerForsendelseProducerImpl(Queue queue, String encryptionPassphrase) {
		this.queue = queue;
		this.encryptionPassphrase = encryptionPassphrase;
	}

	@Override
	public void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		jmsTemplate.send(
				queue,
				session -> session.createTextMessage(marshalHentDokumenterFraJoarkToXmlString(hentDokumenterFraJoark, bestillingsId)));
		log.info("hentDokumenterFraJoark bestilling ble lagt på kø imot qdist012 for bestillingsId={}", bestillingsId);
	}

	private String marshalHentDokumenterFraJoarkToXmlString(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(HentDokumenterFraJoark.class);
			Marshaller marshaller = jaxbContext.createMarshaller();

			StringWriter sw = new StringWriter();
			marshaller.marshal(hentDokumenterFraJoark, sw);

			return encrypt(sw.toString(), bestillingsId);
		} catch (JAXBException | IllegalArgumentException e) {
			throw new MarshallingHentDokumenterFraJoarkTechnicalException("Kunne ikke marshalle hentDokumenterFraJoark til xmlString", e);
		}
	}

	private String encrypt(String plaintext, String key) {
		return new Crypto(encryptionPassphrase, key).encrypt(plaintext);
	}

}
