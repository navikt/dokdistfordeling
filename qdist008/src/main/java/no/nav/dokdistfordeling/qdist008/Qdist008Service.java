package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDistribusjonskanal;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterDistribusjonsinfoTo;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseRequestTo;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.tjoark110.ArkiverDokumentproduksjon;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.qdist008.domain.PersisterForsendelseToRequestMapper;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.LOKAL_PRINT;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistfordeling.qdist008.metrics.MetricUpdater.updateQdist008Metrics;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;
import static org.apache.cxf.common.util.StringUtils.isEmpty;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist008Service {

	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final ArkiverDokumentproduksjon arkiverDokumentproduksjon;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final BestemDistribusjonskanal bestemDistribusjonskanal;
	private final AdministrerForsendelse administrerForsendelse;
	private final PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper;
	private final JournalpostApi journalpostApi;

	@Inject
	public Qdist008Service(PdlGraphQLConsumer pdlGraphQLConsumer,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   AdministrerForsendelse administrerForsendelse,
						   PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper,
						   JournalpostApi journalpostApi) {
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseToRequestMapper = persisterForsendelseToRequestMapper;
		this.journalpostApi = journalpostApi;
	}

	@Handler
	public DistribuerTilKanal distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);
		final String mottakerFnr = getFnr(distribusjonbestilling.getMottaker());

		final DistribusjonsKanalCode distribusjonsKanal = DistribusjonsKanalCode.valueOf(distribusjonbestilling.getDistribusjonKanal());
		exchange.setProperty(PROPERTY_DISTRIBUSJONSKANAL, distribusjonbestilling.getDistribusjonKanal());

		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();

		if (!(INGEN_DISTRIBUSJON.equals(distribusjonsKanal) || LOKAL_PRINT.equals(distribusjonsKanal))) {

			final PersisterForsendelseRequestTo persisterForsendelseRequestTo = persisterForsendelseToRequestMapper
					.map(distribusjonbestilling, dokumenttypeInfoTo, mottakerFnr, distribusjonsKanal);

			PersisterForsendelseResponseTo persisterForsendelseResponseTo = administrerForsendelse.persisterForsendelse(persisterForsendelseRequestTo);
			exchange.setProperty(PROPERTY_FORSENDELSE_ID, persisterForsendelseResponseTo.getForsendelseId());

			distribuerTilKanal.setForsendelseId(persisterForsendelseResponseTo.getForsendelseId());
		}

		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);
		updateQdist008Metrics(distribusjonbestilling);

		return distribuerTilKanal;
	}

	private DokumenttypeInfoTo getTittelFromDokkkatIfNotProvided(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		if (isEmpty(distribusjonbestilling.getForsendelseTittel())) {
			return dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(distribusjonbestilling));
		} else {
			return null;
		}
	}

	private String getFnr(DistribuerForsendelseTo.AktoerTo aktoer) {
		if (aktoer.isIdentifikatorAktoerId()) {
			return pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(aktoer.getIdentifikator());
		} else {
			return aktoer.getIdentifikator();
		}
	}

	private void updateArkivIfArkivsystemIsJoark(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling, DistribusjonsKanalCode distribusjonsKanal) {
		final DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon = distribusjonbestilling.getArkivInformasjon();
		if (arkivInformasjon != null && arkivInformasjon.getArkivSystem().equals(ArkivSystemCode.JOARK)) {

			journalpostApi.oppdaterDistribusjonsinfo(
					arkivInformasjon.getArkivId(),
					OppdaterDistribusjonsinfoTo.builder()
							.settStatusEkspedert(false)
							.utsendingsKanal(distribusjonsKanal.getJoarkUtsendingsKanal())
							.build());
		}
	}

}
