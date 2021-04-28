package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;
import static no.nav.dokdistfordeling.qdist008.metrics.MetricUpdater.updateQdist008Metrics;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;
import static org.springframework.util.StringUtils.isEmpty;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDistribusjonskanal;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.DokDistKanalRequest;
import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseRequestTo;
import no.nav.dokdistfordeling.consumer.rdist001.PersisterForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.tjoark110.ArkiverDokumentproduksjon;
import no.nav.dokdistfordeling.consumer.tjoark110.SettJournalpostAttributterRequestTo;
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

	@Inject
	public Qdist008Service(PdlGraphQLConsumer pdlGraphQLConsumer,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   AdministrerForsendelse administrerForsendelse,
						   PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper) {
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseToRequestMapper = persisterForsendelseToRequestMapper;
	}

	@Handler
	public DistribuerTilKanal distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);
		final String mottakerFnr = getFnr(distribusjonbestilling.getMottaker());
		final String brukerFnr = getFnr(distribusjonbestilling.getBruker());

		final DistribusjonsKanalCode distribusjonsKanal = bestemDistribusjonskanal.bestemKanal(
				mapDokDistKanalRequest(distribusjonbestilling, mottakerFnr, brukerFnr));
		exchange.setProperty(PROPERTY_DISTRIBUSJONSKANAL, distribusjonsKanal);

		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();

		if (!(distribusjonsKanal.equals(DistribusjonsKanalCode.INGEN_DISTRIBUSJON) || distribusjonsKanal.equals(DistribusjonsKanalCode.LOKAL_PRINT))) {

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
			arkiverDokumentproduksjon.settJournalpostAttributter(SettJournalpostAttributterRequestTo.builder()
							.journalpostId(arkivInformasjon.getArkivId())
							.utsendingskanal(distribusjonsKanal.getJoarkUtsendingsKanal())
							.build(),
					SERVICE_ID);
		}
	}

	private String getIdentifikator(DistribuerForsendelseTo.AktoerTo aktoerTo, String fnrMottaker) {
		if (aktoerTo.isIdentifikatorAktoerId()) {
			return fnrMottaker;
		}
		return aktoerTo.getIdentifikator();
	}

	private DokDistKanalRequest mapDokDistKanalRequest(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo,
													   String fnrMottaker,
													   String fnrBruker) {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(getDokumenttypeIdHoveddokument(distribusjonbestillingTo))
				.mottakerId(getIdentifikator(distribusjonbestillingTo.getMottaker(), fnrMottaker))
				.mottakerType(distribusjonbestillingTo.getMottaker().getAktoerType().name())
				.brukerId(getIdentifikator(distribusjonbestillingTo.getBruker(), fnrBruker))
				.erArkivert(distribusjonbestillingTo.getArkivInformasjon() != null)
				.build();
	}
}
