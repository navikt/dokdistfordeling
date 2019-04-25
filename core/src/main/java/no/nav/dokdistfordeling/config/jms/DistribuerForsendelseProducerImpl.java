package no.nav.dokdistfordeling.config.jms;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.exception.technical.MarshalHentDokumenterFraJoarkTechnicalException;

import no.nav.dokdistfordeling.melding.qdist012.HentDokumenterFraJoark;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

@Slf4j
public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {

	private JmsTemplate jmsTemplate;
	private String encryptionPassphrase;
	private Queue queue;

	public DistribuerForsendelseProducerImpl(JmsTemplate jmsTemplate, Queue queue, String encryptionPassphrase) {
		this.jmsTemplate = jmsTemplate;
		this.queue = queue;
		this.encryptionPassphrase = encryptionPassphrase;
	}

	@Override
	public void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		jmsTemplate.send(
				queue,
				session -> session.createTextMessage(marshalHentDokumenterFraJoarkToXmlString(hentDokumenterFraJoark, bestillingsId)));
		log.info("hentDokumenterFraJoark bestilling med bestillingsId{} ble lagt på kø imot qdist012", bestillingsId);
	}

	private String marshalHentDokumenterFraJoarkToXmlString(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(HentDokumenterFraJoark.class);
			Marshaller marshaller = jaxbContext.createMarshaller();

			StringWriter sw = new StringWriter();
			marshaller.marshal(hentDokumenterFraJoark, sw);

			return encrypt(sw.toString(), bestillingsId);
		} catch (JAXBException e) {
			throw new MarshalHentDokumenterFraJoarkTechnicalException("Kunne ikke marshalle hentDokumenterFraJoark bestilling til xmlString", e);
		}
	}

	private String encrypt(String plaintext, String key) {
		return new Crypto(encryptionPassphrase, key).encrypt(plaintext);
	}

}
