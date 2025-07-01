package no.nav.dokdistfordeling.qdist008;

import jakarta.jms.Queue;
import jakarta.xml.bind.JAXBContext;
import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.exception.functional.JournalpostFeilregistrertException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DPO;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.SDP;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.TRYGDERETTEN;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;


@Component
public class Qdist008Route extends RouteBuilder {

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
	private final Queue qdist011;
	private final Queue qdist013;
	private final Queue qdist015;
	private final Queue qdist016;
	private final Queue qdist008FunksjonellFeil;

	public Qdist008Route(Queue qdist008,
						 Queue qdist009,
						 Queue qdist010,
						 Queue qdist011,
						 Queue qdist013,
						 Queue qdist015,
						 Queue qdist016,
						 Queue qdist008FunksjonellFeil,
						 Qdist008Service qdist008Service,
						 DistribuerForsendelseMapper distribuerForsendelseMapper,
						 ForsendelseValidator forsendelseValidator,
						 DokdistStatusUpdater dokdistStatusUpdater) {
		this.qdist008 = qdist008;
		this.qdist009 = qdist009;
		this.qdist010 = qdist010;
		this.qdist011 = qdist011;
		this.qdist013 = qdist013;
		this.qdist015 = qdist015;
		this.qdist016 = qdist016;
		this.qdist008FunksjonellFeil = qdist008FunksjonellFeil;
		this.qdist008Service = qdist008Service;
		this.distribuerForsendelseMapper = distribuerForsendelseMapper;
		this.forsendelseValidator = forsendelseValidator;
		this.dokdistStatusUpdater = dokdistStatusUpdater;
	}

	@Override
	public void configure() throws Exception {
		//@formatter:off
		errorHandler(defaultErrorHandler()
				.maximumRedeliveries(0)
				.log(log)
				.logExhaustedMessageBody(false)
				.logStackTrace(true)
				.loggingLevel(ERROR));

		onException(AbstractDokdistfordelingFunctionalException.class, ValidationException.class)
				.handled(true)
				.useOriginalMessage()
				.log(WARN, log, "Legger melding på funksjonell backoutkø for qdist008 for " + getIdsForLogging() + " grunnet=${exception}")
				.to("jms:" + qdist008FunksjonellFeil.getQueueName());

		//Om journalposten er feilregistrert skal den forkastes og ikke forsøkes distribuert
		onException(JournalpostFeilregistrertException.class)
				.handled(true)
				.log(WARN, log, "Forkaster melding på qdist008 for " + getIdsForLogging() + " grunnet=${exception}")
				.end();

		from("jms:" + qdist008.getQueueName() +
				"?transacted=true")
				.routeId(SERVICE_ID)
				.setExchangePattern(InOnly)
				.onCompletion()
					.process(exchange -> MDC.clear())
				.end() // end of onCompletion
				.process(new IdsProcessor())
				.log(INFO, log, format("qdist008 har mottatt forsendelse med bestillingsId=${exchangeProperty.%s}.", PROPERTY_BESTILLINGS_ID))
				.to("validator:no/nav/meldinger/virksomhet/dokdistfordeling/xsd/qdist008/in/distribuerforsendelse.xsd")
				.unmarshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerForsendelse.class)))
				.bean(distribuerForsendelseMapper)
				.bean(forsendelseValidator)
				.log(INFO, log, format("qdist008 har validert forsendelse med bestillingsId=${exchangeProperty.%s}.", PROPERTY_BESTILLINGS_ID))
				.bean(qdist008Service)
				.marshal(new JaxbDataFormat(JAXBContext.newInstance(DistribuerTilKanal.class)))
				.convertBodyTo(String.class, UTF_8.toString())
				.choice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(LOKAL_PRINT))
					.log(INFO, log, format("avslutter behandling av forsendelse med %s. Distribusjonskanal=LOKAL_PRINT", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(INGEN_DISTRIBUSJON))
					.log(INFO, log, format("avslutter behandling av forsendelse med %s. Distribusjonskanal=INGEN_DISTRIBUSJON", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(PRINT))
					.to(InOnly,"jms:" + qdist009.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist009 for distribusjon via PRINT", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DITTNAV))
					.to(InOnly, "jms:" + qdist010.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist010 for distribusjon via DITT NAV", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(SDP))
					.to(InOnly, "jms:" + qdist011.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist011 for distribusjon via DPI", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(TRYGDERETTEN))
					.to(InOnly,"jms:" + qdist013.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist013 for distribusjon via Trygderetten", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DPVT))
					.to(InOnly, "jms:" + qdist016.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist016 for distribusjon via DPVT", getIdsForLogging()))
					.endChoice()
				.when(exchangeProperty(PROPERTY_DISTRIBUSJONSKANAL).isEqualTo(DPO))
					.to(InOnly, "jms:" + qdist015.getQueueName())
					.log(INFO, log, format("qdist008 har lagt forsendelse med %s på kø til qdist015 for distribusjon via DPO", getIdsForLogging()))
					.endChoice()
				.end()
				.bean(dokdistStatusUpdater)
				.log(INFO, log, format("qdist008 har oppdatert forsendelseStatus i dokdist og avslutter behandling av forsendelse med %s", getIdsForLogging()));
		//@formatter:on
	}

	public static String getIdsForLogging() {
		return format("bestillingsId=${exchangeProperty.%s} og " +
				"forsendelseId=${exchangeProperty.%s}", PROPERTY_BESTILLINGS_ID, PROPERTY_FORSENDELSE_ID);
	}
}
