package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.constants.MdcConstants.CALL_ID;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.prometheus.Qdist008MetricsRoutePolicy;
import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import java.nio.charset.StandardCharsets;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class Qdist008Route extends SpringRouteBuilder {

	public final MeterRegistry registry;
	public static final String SERVICE_ID = "qdist008";
	private static final String PROPERTY_ORIGINAL_PAYLOAD = "qdok008OriginalPayload";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
	public static final String PROPERTY_ORIGINAL_MESSAGE = "originalMessage";

	private final Qdist008Service qdist008Service;
	private final DistribuerForsendelseMapper distribuerForsendelseMapper;
	private final ForsendelseValidator forsendelseValidator;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist008;
	private final Queue qdist009;
	private final Queue qdist008FunksjonellFeil;

	@Inject
	public Qdist008Route(Qdist008Service qdist008Service,
						 DistribuerForsendelseMapper distribuerForsendelseMapper,
						 ForsendelseValidator forsendelseValidator,
						 DokdistStatusUpdater dokdistStatusUpdater,
						 MeterRegistry registry,
						 Queue qdist008,
						 Queue qdist009,
						 Queue qdist008FunksjonellFeil) {
		this.qdist008Service = qdist008Service;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
		this.forsendelseValidator = forsendelseValidator;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist008 = qdist008;
		this.qdist009 = qdist009;
		this.qdist008FunksjonellFeil = qdist008FunksjonellFeil;
		this.registry = registry;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(LoggingLevel.ERROR));

		onException(DokdistfordelingFunctionalException.class, ValidationException.class)
				.handled(true)
				.setBody(exchangeProperty(PROPERTY_ORIGINAL_PAYLOAD))
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist008FunksjonellFeil.getQueueName());

		from("jms:" + qdist008.getQueueName())
				.routeId(SERVICE_ID)
				.routePolicy(new Qdist008MetricsRoutePolicy(registry))
				.setProperty(PROPERTY_ORIGINAL_MESSAGE, simple("${body}"))
				.setExchangePattern(ExchangePattern.InOnly)
				.doTry()
				.setProperty(PROPERTY_BESTILLINGS_ID, xpath("//bestillingsId/text()", String.class))
				.log(LoggingLevel.INFO, log, "qdist008 har mottatt forsendelse med bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.")
				.process(exchange -> MDC.put(CALL_ID, (String) exchange.getProperty(PROPERTY_BESTILLINGS_ID)))
				.setProperty(PROPERTY_ORIGINAL_PAYLOAD, simple("${body}"))
				.doCatch(Exception.class)
				.end()
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.log(LoggingLevel.INFO, log, "qdist008 har validert forsendelse med bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} ok.")
				.bean(qdist008Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class)))
				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
				.inOnly("jms:" + qdist009.getQueueName())
				.log(LoggingLevel.INFO, log, "qdist008 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist009 for distribusjon via PRINT")
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, "qdist008 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med " + getIdsForLogging());
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
