package no.nav.dokdistfordeling.qdist008;

import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist008Route extends SpringRouteBuilder {

	public static final String SERVICE_ID = "qdist008";

	private final Qdist008Service qdist008Service;
	private final DistribuerForsendelseMapper distribuerForsendelseMapper;
	private final ForsendelseValidator forsendelseValidator;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist008;

	@Inject
	public Qdist008Route(Qdist008Service qdist008Service,
						 DistribuerForsendelseMapper distribuerForsendelseMapper,
						 ForsendelseValidator forsendelseValidator,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 Queue qdist008) {
		this.qdist008Service = qdist008Service;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
		this.forsendelseValidator = forsendelseValidator;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist008 = qdist008;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.retryAttemptedLogLevel(LoggingLevel.INFO)
				.logRetryStackTrace(false)
				.logExhaustedMessageBody(true)
				.loggingLevel(LoggingLevel.ERROR));

		from("jms:" + qdist008.getQueueName() +
				"?transacted=true" +
				"&cacheLevelName=CACHE_CONSUMER" +
				"&errorHandlerLogStackTrace=false" +
				"&errorHandlerLoggingLevel=DEBUG")
				.routeId(SERVICE_ID)
				.log(LoggingLevel.INFO, log, "Melding mottatt")
//				.process(new MDCContextProcessor(BREVOGARKIV_USER_ID))
//				.setBody(bodyAs(TextMessage.class).method("getText"))
//				.setProperty(PROPERTY_ORIGINAL_PAYLOAD, body())
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.log(LoggingLevel.INFO, log, "Melding unmarshaled")
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.bean(qdist008Service)
//				.setProperty(PROPERTY_FORSENDELSE_ID, simple("${body.bestillingsId}"))
//				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class)))
//				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
//				.inOnly("jms:" + qdok002 + "?messageConverter=#mdcMessageConverter")
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, "qdist008 har lagt dokumentbestilling med " + getIdsForLogging() + " på kø til qdist009 for print-distribusjon");
	}

	public static String getIdsForLogging() {
		return "";
//				"bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
//				"bestillingsId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
