package no.nav.dokdistfordeling.config.jms;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.exception.technical.MarshalHentDokumenterFraJoarkTechnicalException;

import no.nav.dokdistfordeling.melding.qdist012.HentDokumenterFraJoark;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

@Slf4j
@Component
public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {

	private JmsTemplate jmsTemplate;

	@Value("${hentdokumenter_fra_joark_crypto_password}")
	private String encryptionPassphrase;

	private Queue qdist012;

	public DistribuerForsendelseProducerImpl(JmsTemplate jmsTemplate, Queue qdist012) {
		this.jmsTemplate = jmsTemplate;
		this.qdist012 = qdist012;
	}

	@Override
	public void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		jmsTemplate.send(
				qdist012,
				session -> session.createTextMessage(marshalHentDokumenterFraJoarkToXmlStringAndEncrypt(hentDokumenterFraJoark, bestillingsId)));
		log.info("hentDokumenterFraJoark bestilling med bestillingsId{} ble lagt på kø imot qdist012", bestillingsId);
	}

	private String marshalHentDokumenterFraJoarkToXmlStringAndEncrypt(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
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
