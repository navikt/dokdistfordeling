package no.nav.dokdistfordeling.qdist008;

import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import org.apache.camel.LoggingLevel;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.spring.SpringRouteBuilder;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;


/**
 * @author Sigurd Midttun, Visma Consulting.
 */
public class Qdist008Route extends SpringRouteBuilder {

	public static final String SERVICE_ID = "qdist008";

	private final Qdist008Service qdist008Service;
	private final DistribuerForsendelseMapper distribuerForsendelseMapper;
	private final ForsendelseValidator forsendelseValidator;

	@Inject
	public Qdist008Route(Qdist008Service qdist008Service,
						 DistribuerForsendelseMapper distribuerForsendelseMapper, ForsendelseValidator forsendelseValidator) {
		this.qdist008Service = qdist008Service;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
		this.forsendelseValidator = forsendelseValidator;
	}

	@Override
	public void configure() throws Exception {
		// Propager feil direkte til konsument
		errorHandler(noErrorHandler());

		from("QDOK001_ENDPOINT")
				.routeId(SERVICE_ID)
//				.process(new MDCContextProcessor(BREVOGARKIV_USER_ID))
//				.setBody(bodyAs(TextMessage.class).method("getText"))
//				.setProperty(PROPERTY_ORIGINAL_PAYLOAD, body())
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.bean(qdist008Service)
//				.setProperty(PROPERTY_FORSENDELSE_ID, simple("${body.forsendelseId}"))
//				.marshal(new JaxbDataFormat(JAXBContext.newInstance(KlargjoerForArkivering.class)))
//				.convertBodyTo(String.class, StandardCharsets.UTF_8.toString())
//				.inOnly("jms:" + qdok002 + "?messageConverter=#mdcMessageConverter")
				.log(LoggingLevel.INFO, log, "qdist008 har lagt dokumentbestilling med " + getIdsForLogging() + " på kø til qdist009 for print-distribusjon");
	}

	public static String getIdsForLogging() {
		return "";
//				"bestillingsId=${exchangeProperty." + PROPERTY_BESTILLINGS_ID + "}, " +
//				"forsendelseId=${exchangeProperty." + PROPERTY_FORSENDELSE_ID + "}";
	}
}
