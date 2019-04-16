package no.nav.dokdistfordeling.qdist012;

import static no.nav.dokdistfordeling.constants.MdcConstants.CALL_ID;
import static org.apache.camel.LoggingLevel.ERROR;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.metrics.Qdist012MetricsRoutePolicy;
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
//FIXME
@Component
public class Qdist012Route extends SpringRouteBuilder {

	public static final String QDIST012_SERVICE_ID = "qdist012";
	static final String PROPERTY_BESTILLINGS_ID = "bestillingsId";
	static final String PROPERTY_FORSENDELSE_ID = "forsendelseId";

	private final Qdist012Service qdist012Service;
	private final Queue qdist012;
	private final Queue qdist012FunksjonellFeil;
	private final Queue qdist008;
	private final Qdist012MetricsRoutePolicy qdist012MetricsRoutePolicy;


	@Inject
	public Qdist012Route(Queue qdist012,
						 Queue qdist012FunksjonellFeil,
						 Queue qdist008,
						 Qdist012Service qdist012Service,
						 Qdist012MetricsRoutePolicy qdist012MetricsRoutePolicy) {
		this.qdist012 = qdist012;
		this.qdist012FunksjonellFeil = qdist012FunksjonellFeil;
		this.qdist008 = qdist008;
		this.qdist012Service = qdist012Service;
		this.qdist012MetricsRoutePolicy = qdist012MetricsRoutePolicy;
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
				.to("jms:" + qdist012FunksjonellFeil.getQueueName());

		from("jms:" + qdist012.getQueueName() +
				"?transacted=true")
				.routeId(QDIST012_SERVICE_ID)
				.routePolicy(qdist012MetricsRoutePolicy)
				.setExchangePattern(ExchangePattern.InOnly)
				.doTry()
				.setProperty(PROPERTY_BESTILLINGS_ID, xpath("//bestillingsId/text()", String.class))
				.log(LoggingLevel.INFO, log, "qdist012 har mottatt forsendelse med bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}.")
				.process(exchange -> MDC.put(CALL_ID, (String) exchange.getProperty(PROPERTY_BESTILLINGS_ID)))
				.doCatch(Exception.class)
				.end()
				.to("validator:no/nav/dokdistfordeling/hentdokumenterfrajoark/no/nav/dokdistfordeling/qdist012/HentDokumenterFraJoark.java")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(HentDokumenterFraJoark.class)))
				.bean(qdist012Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
				.setHeader(CALL_ID, simple("${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}"))
				.inOnly("jms:" + qdist008.getQueueName())
				.log(LoggingLevel.INFO, log, "qdist012 har lagt forsendelse med " + getIdsForLogging() + " på kø til qdist008 for distribusjon av forsendelse")
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/distribuerforsendelse.xsd");
	}

	public static String getIdsForLogging() {
		return "bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "} og " +
				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
