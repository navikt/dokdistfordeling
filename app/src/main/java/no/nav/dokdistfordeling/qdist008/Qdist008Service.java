package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.prometheus.PrometheusLabels.LABEL_PROCESS;
import static no.nav.dokdistfordeling.prometheus.Qdist008MetricsRoutePolicy.QDIST008;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static org.springframework.util.StringUtils.isEmpty;

import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistfordeling.consumer.aktoerv2.Aktoer;
import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDistribusjonskanal;
import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseRequestTo;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseToRequestMapper;
import no.nav.dokdistfordeling.consumer.tjoark110.ArkiverDokumentproduksjon;
import no.nav.dokdistfordeling.consumer.tjoark110.SettJournalpostAttributterRequestTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist008Service {

	private final String QDIST008_SERVICE = "service";
	private final String QDIST008_HOVEDDOKUMENT = "hoveddokument";
	private final String QDIST008_VEDLEGG = "vedlegg";
	private final String LABEL_TEMA = "tema";
	private final String LABEL_DOKUMENTTYPEID = "dokumenttypeid";
	private final String LABEL_BESTILLENDE_FAGSYSTEM = "bestillende_fagsystem";


	private final Aktoer aktoer;
	private final ArkiverDokumentproduksjon arkiverDokumentproduksjon;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final BestemDistribusjonskanal bestemDistribusjonskanal;
	private final AdministrerForsendelse administrerForsendelse;
	private final PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper;
	private final MeterRegistry registry;

	@Inject
	public Qdist008Service(Aktoer aktoer,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   AdministrerForsendelse administrerForsendelse,
						   PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper,
						   MeterRegistry registry) {
		this.aktoer = aktoer;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseToRequestMapper = persisterForsendelseToRequestMapper;
		this.registry = registry;
	}

	@Handler
	public DistribuerForsendelseTilSentralPrint distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);
		final HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo = getFoedselsnummerIfMottakerIdentifikatorIsAktoerId(distribusjonbestilling
				.getMottaker());
		final DistribusjonsKanalCode distribusjonsKanal = bestemDistribusjonskanal.bestemKanal();

		final PersisterForsendelseRequestTo persisterForsendelseRequestTo = persisterForsendelseToRequestMapper
				.map(distribusjonbestilling, dokumenttypeInfoTo, hentIdentForAktoerIdResponseTo, distribusjonsKanal);

		updateMetrics(persisterForsendelseRequestTo, distribusjonbestilling);

		PersisterForsendelseResponseTo persisterForsendelseResponseTo = administrerForsendelse.persisterForsendelse(persisterForsendelseRequestTo);
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, persisterForsendelseResponseTo.getForsendelseId());

		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);

		return new DistribuerForsendelseTilSentralPrint(persisterForsendelseResponseTo.getForsendelseId());
	}

	private String getDokumenttypeIdHoveddokument(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.HOVEDDOKUMENT))
				.map(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId)
				.collect(Collectors.toList())
				.get(0);
	}

	private DokumenttypeInfoTo getTittelFromDokkkatIfNotProvided(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		if (isEmpty(distribusjonbestilling.getForsendelseTittel())) {
			return dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(distribusjonbestilling));
		} else {
			return null;
		}
	}

	private HentIdentForAktoerIdResponseTo getFoedselsnummerIfMottakerIdentifikatorIsAktoerId(DistribuerForsendelseTo.MottakerTo mottaker) {
		if (mottaker.isIdentifikatorAktoerId()) {
			return aktoer.hentIdentForAktoerId(mottaker.getIdentifikator());
		} else {
			return null;
		}
	}

	private void updateArkivIfArkivsystemIsJoark(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling, DistribusjonsKanalCode distribusjonsKanal) {
		final DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon = distribusjonbestilling.getArkivInformasjon();
		if (arkivInformasjon != null && arkivInformasjon.getArkivSystem().equals(ArkivSystemCode.JOARK)) {
			arkiverDokumentproduksjon.settJournalpostAttributter(SettJournalpostAttributterRequestTo.builder()
					.journalpostId(arkivInformasjon.getArkivId())
					.utsendingskanal(distribusjonsKanal.getJoarkUtsendingsKanal())
					.build());
		}
	}

	private void updateMetrics(PersisterForsendelseRequestTo persisterForsendelseRequestTo,
							   DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		registry.counter(QDIST008_SERVICE,
				LABEL_PROCESS, QDIST008,
				LABEL_DOKUMENTTYPEID, getDokumenttypeIdHoveddokument(distribusjonbestilling),
				LABEL_TEMA, persisterForsendelseRequestTo.getTema() == null ? "mangler_tema" : persisterForsendelseRequestTo.getTema().toString(),
				LABEL_BESTILLENDE_FAGSYSTEM, persisterForsendelseRequestTo.getBestillendeFagsystem()).increment();

		registry.counter(QDIST008_HOVEDDOKUMENT,
				LABEL_PROCESS, QDIST008).increment();

		registry.counter(QDIST008_VEDLEGG,
				LABEL_PROCESS, QDIST008).increment(countVedlegg(distribusjonbestilling));
	}

	private int countVedlegg(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		return distribusjonbestilling.getDokumenter().stream()
				.filter(dokumentInformasjonTo -> dokumentInformasjonTo.getTilknyttetSom()
						.equals(TilknyttetSomCode.VEDLEGG))
				.collect(Collectors.toList()).size();
	}

}
