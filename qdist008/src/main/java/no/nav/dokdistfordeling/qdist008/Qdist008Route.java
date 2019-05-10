package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;
import static org.apache.camel.LoggingLevel.ERROR;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.qdist008.metrics.Qdist008MetricsRoutePolicy;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ValidationException;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;
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
	static final String PROPERTY_DISTRIBUSJONS_OBJEKT = "distribusjonsObjekt";

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
				.process(new IdsProcessor())
				.log(LoggingLevel.INFO, log, String.format("qdist008 har mottatt forsendelse med bestillingsId=${exchangeProperty.%s}.", PROPERTY_BESTILLINGS_ID))
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/in/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.log(LoggingLevel.INFO, log, String.format("qdist008 har validert forsendelse med bestillingsId=${exchangeProperty. %s} ok.", PROPERTY_BESTILLINGS_ID))
				.bean(qdist008Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
				.setHeader(CALL_ID, simple("${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}"))
				.choice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.LOKAL_PRINT))
					.log(LoggingLevel.INFO, log, String.format("avslutter behandling av forsendelse med %s. Distribusjonskanal=LOKAL_PRINT", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.INGEN_DISTRIBUSJON))
					.log(LoggingLevel.INFO, log, String.format("Avslutter behandling av forsendelse med %s. Distribusjonskanal=INGEN_DISTRIBUSJON", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.PRINT))
					.inOnly("jms:" + qdist009.getQueueName())
					.log(LoggingLevel.INFO, log, String.format("qdist008 har lagt forsendelse med %s på kø til qdist009 for distribusjon via PRINT", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.DITTNAV))
					.inOnly("jms:" + qdist010.getQueueName())
					.log(LoggingLevel.INFO, log, String.format("qdist008 har lagt forsendelse med %s på kø til qdist010 for distribusjon via DITT NAV", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DistribusjonsKanalCode.SDP))
					//TODO Legg til .inOnly("jms:" + qdist011.getQueueName()) når qdist011 er opprettet, og endre log.
					.log(LoggingLevel.WARN, log, String.format("qdist008 skulle ha lagt forsendelse med %s på kø til qdist011 for distribusjon via DPI", getIdsForLogging()))
					.endChoice()
				.end()
				.bean(dokdistStatusUpdater)
				.log(LoggingLevel.INFO, log, String.format("qdist008 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med %s", getIdsForLogging()));
	}

	public static String getIdsForLogging() {
		return String.format("bestillingsId=${exchangeProperty.%s} og " +
				"forsendelseId=${exchangeProperty.%s}", PROPERTY_BESTILLINGS_ID, PROPERTY_FORSENDELSE_ID);
	}
}
