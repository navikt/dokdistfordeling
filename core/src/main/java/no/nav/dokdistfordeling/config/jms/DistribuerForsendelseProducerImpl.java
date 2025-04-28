package no.nav.dokdistfordeling.config.jms;

import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.Constants.JOURNALPOST_ID;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.crypto.Crypto;
import no.nav.dokdistfordeling.exception.technical.MarshalHentDokumenterFraJoarkTechnicalException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

@Slf4j
@Component
public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {

	private final String encryptionPassphrase;
	private final JmsTemplate jmsTemplate;
	private final JAXBContext jaxbContext;
	private final Queue qdist012;

	public DistribuerForsendelseProducerImpl(JmsTemplate jmsTemplate,
											 Queue qdist012,
											 @Value("${hentdokumenter_fra_joark_crypto_password}") String encryptionPassphrase) {
		this.encryptionPassphrase = encryptionPassphrase;
		this.jmsTemplate = jmsTemplate;
		try {
			this.jaxbContext = JAXBContext.newInstance(HentDokumenterFraJoark.class);
		} catch (JAXBException e) {
			throw new MarshalHentDokumenterFraJoarkTechnicalException("Kunne ikke sette opp JAXBContext", e);
		}
		this.qdist012 = qdist012;
	}

	@Override
	public void produce(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId, String journalpostId) {
		jmsTemplate.send(
				qdist012,
				session -> {
					TextMessage msg = session.createTextMessage(marshalHentDokumenterFraJoarkToXmlStringAndEncrypt(hentDokumenterFraJoark, bestillingsId));
					msg.setStringProperty(CALL_ID, MDC.get(CALL_ID));
					if (MDC.get(CONSUMER_ID) != null) {
						msg.setStringProperty(CONSUMER_ID, MDC.get(CONSUMER_ID));
					}
					msg.setStringProperty(BESTILLINGS_ID, bestillingsId);
					msg.setStringProperty(JOURNALPOST_ID, journalpostId);
					msg.setBooleanProperty("plaintext", false);
					return msg;
				});
		log.info("hentDokumenterFraJoark bestilling med bestillingsId={} ble lagt på kø imot qdist012", bestillingsId);
	}

	private String marshalHentDokumenterFraJoarkToXmlStringAndEncrypt(HentDokumenterFraJoark hentDokumenterFraJoark, String bestillingsId) {
		try {
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
