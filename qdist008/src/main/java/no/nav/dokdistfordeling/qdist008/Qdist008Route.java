package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static org.apache.camel.LoggingLevel.ERROR;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTilSentralPrint;
import no.nav.dokdistfordeling.qdist008.metrics.Qdist008MetricsRoutePolicy;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
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

	public static final String SERVICE_ID = "qdist008";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";
	static final String PROPERTY_DISTRIBUSJONSKANAL = "distribusjonskanal";

	private final Qdist008Service qdist008Service;
	private final DistribuerForsendelseMapper distribuerForsendelseMapper;
	private final ForsendelseValidator forsendelseValidator;
	private final DokdistStatusUpdater dokdistStatusUpdater;
	private final Queue qdist008;
	private final Queue qdist009;
	private final Queue qdist010;
	private final Queue qdist008FunksjonellFeil;
	private final Qdist008MetricsRoutePolicy qdist008MetricsRoutePolicy;

	@Inject
	public Qdist008Route(Queue qdist008,
						 Queue qdist009,
						 Queue qdist010,
						 Queue qdist008FunksjonellFeil,
						 Qdist008Service qdist008Service,
						 Qdist008MetricsRoutePolicy qdist008MetricsRoutePolicy,
						 DistribuerForsendelseMapper distribuerForsendelseMapper,
						 ForsendelseValidator forsendelseValidator,
						 DokdistStatusUpdater dokdistStatusUpdater) {
		this.qdist008 = qdist008;
		this.qdist009 = qdist009;
		this.qdist010 = qdist010;
		this.qdist008FunksjonellFeil = qdist008FunksjonellFeil;
		this.qdist008Service = qdist008Service;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
		this.forsendelseValidator = forsendelseValidator;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
		this.qdist008MetricsRoutePolicy = qdist008MetricsRoutePolicy;
	}

	@Override
	public void configure() throws Exception {
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(true)
				.loggingLevel(ERROR));

		onException(AbstractDokdistfordelingFunctionalException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.log(LoggingLevel.WARN, log, "${exception}; " + getIdsForLogging())
				.to("jms:" + qdist008FunksjonellFeil.getQueueName());

		from("jms:" + qdist008.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.routePolicy(qdist008MetricsRoutePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.doTry()
				.setProperty(PROPERTY_BESTILLINGS_ID, xpath("//bestillingsId/text()", String.class))
				.log(LoggingLevel.INFO, log, "qdist008 har mottatt forsendelse med bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.")
				.process(exchange -> MDC.put(CALL_ID, (String) exchange.getProperty(PROPERTY_BESTILLINGS_ID)))
				.doCatch(Exception.class)
				.end()
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/in/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.log(LoggingLevel.INFO, log, "qdist008 har validert forsendelse med bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} ok.")
				.bean(qdist008Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelseTilSentralPrint.class)))
				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
				.setHeader(CALL_ID, simple("${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}"))
				.choice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.PRINT))
					.inOnly("jms:" + qdist009.getQueueName())
					.log(LoggingLevel.INFO, log, "qdist008 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist009 for distribusjon via PRINT")
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.DITT_NAV))
					.inOnly("jms:" + qdist010.getQueueName())
					.log(LoggingLevel.INFO, log, "qdist008 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist010 for distribusjon via DITT NAV")
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.SDP))
					.log(LoggingLevel.WARN, log, "qdist008 skulle ha lagt forsendelse med " + getIdsForLogging() + " på kø til qdist011 for distribusjon via DPI")
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.LOKAL_PRINT))
					.log(LoggingLevel.INFO, log, "qdist008 forsendelse med " + getIdsForLogging() + " er printet ut lokalt. Behandling avsluttes.")
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.INGEN_DISTRIBUSJON))
					.log(LoggingLevel.INFO, log, "qdist008 forsendelse med " + getIdsForLogging() + " skal ikke distribueres. Behandling avsluttes.")
				.end()
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, "qdist008 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med " + getIdsForLogging());
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
