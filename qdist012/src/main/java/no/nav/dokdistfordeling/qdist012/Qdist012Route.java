package no.nav.dokdistfordeling.qdist012;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.exception.functional.PersonErDoedUkjentAdresseException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

@Component
public class Qdist012Route extends RouteBuilder {

	public static final String QDIST012_SERVICE_ID = "qdist012";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_JOURNALPOST_ID = "journalpostId";

	private final Qdist012Service qdist012Service;
	private final Queue qdist012;
	private final Queue qdist012FunksjonellFeil;
	private final Queue qdist008;
	private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper;
	private final HentDokumenterFraJoarkDecrypter hentDokumenterFraJoarkDecrypter;

	public Qdist012Route(Queue qdist012,
						 Queue qdist012FunksjonellFeil,
						 Queue qdist008,
						 Qdist012Service qdist012Service,
						 HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper,
						 HentDokumenterFraJoarkDecrypter hentDokumenterFraJoarkDecrypter) {
		this.qdist012 = qdist012;
		this.qdist012FunksjonellFeil = qdist012FunksjonellFeil;
		this.qdist008 = qdist008;
		this.qdist012Service = qdist012Service;
		this.hentDokumenterFraJoarkMapper = hentDokumenterFraJoarkMapper;
		this.hentDokumenterFraJoarkDecrypter = hentDokumenterFraJoarkDecrypter;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(false)
				.loggingLevel(ERROR));

		onException(PersonErDoedUkjentAdresseException.class)
				.handled(true)
				.useOriginalMessage()
				.log(WARN, log, "${exception}; " + getIdsForLogging());

		onException(AbstractDokdistfordelingFunctionalException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.log(WARN, log, "${exception}; " + getIdsForLogging())
				.log(WARN, log, "Legger melding på funksjonell backoutkø for qdist012; " + getIdsForLogging())
				.to("jms:" + qdist012FunksjonellFeil.getQueueName());

		from("jms:" + qdist012.getQueueName() +
				"?transacted=true")
				.routeId(QDIST012_SERVICE_ID)
				.setExchangePattern(InOnly)
				.process(new HeaderProcessor())
				.log(INFO, log, "qdist012 har mottatt forsendelse med " + getIdsForLogging())
				.bean(hentDokumenterFraJoarkDecrypter)
				.to("validator:/no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist012/hentdokumenterfrajoark.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(HentDokumenterFraJoark.class)))
				.bean(hentDokumenterFraJoarkMapper)
				.bean(qdist012Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.convertBodyTo(String.class, UTF_8.toString())
				.to(InOnly, "jms:" + qdist008.getQueueName())
				.log(INFO, log, "qdist012 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist008 for distribusjon av forsendelse");
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"journalpostId=${exchangeProperty." + PROPERTY_JOURNALPOST_ID + "}";
	}

}

