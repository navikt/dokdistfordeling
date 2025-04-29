package no.nav.dokdistfordeling.config.jms;

import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.technical.MarshalHentDokumenterFraJoarkTechnicalException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.slf4j.MDC;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

import static no.nav.dokdistfordeling.constants.Constants.BESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static no.nav.dokdistfordeling.constants.Constants.CONSUMER_ID;
import static no.nav.dokdistfordeling.constants.Constants.JOURNALPOST_ID;

@Slf4j
@Component
public class DistribuerForsendelseProducerImpl implements DistribuerForsendelseProducer {

	private final JmsTemplate jmsTemplate;
	private final JAXBContext jaxbContext;
	private final Queue qdist012;

	public DistribuerForsendelseProducerImpl(JmsTemplate jmsTemplate,
											 Queue qdist012) {
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
					TextMessage msg = session.createTextMessage(marshalHentDokumenterFraJoarkToXmlString(hentDokumenterFraJoark));
					msg.setStringProperty(CALL_ID, MDC.get(CALL_ID));
					if (MDC.get(CONSUMER_ID) != null) {
						msg.setStringProperty(CONSUMER_ID, MDC.get(CONSUMER_ID));
					}
					msg.setStringProperty(BESTILLINGS_ID, bestillingsId);
					msg.setStringProperty(JOURNALPOST_ID, journalpostId);
					return msg;
				});
		log.info("hentDokumenterFraJoark bestilling med bestillingsId={} ble lagt på kø imot qdist012", bestillingsId);
	}

	private String marshalHentDokumenterFraJoarkToXmlString(HentDokumenterFraJoark hentDokumenterFraJoark) {
		try {
			Marshaller marshaller = jaxbContext.createMarshaller();
			StringWriter sw = new StringWriter();
			marshaller.marshal(hentDokumenterFraJoark, sw);
			return sw.toString();
		} catch (JAXBException e) {
			throw new MarshalHentDokumenterFraJoarkTechnicalException("Kunne ikke marshalle hentDokumenterFraJoark bestilling til xmlString", e);
		}
	}
}
