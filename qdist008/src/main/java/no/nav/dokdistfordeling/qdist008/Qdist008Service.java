package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONS_OBJEKT;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;
import static no.nav.dokdistfordeling.qdist008.metrics.MetricUpdater.updateQdist008Metrics;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;
import static org.springframework.util.StringUtils.isEmpty;

import no.nav.dokdistfordeling.consumer.aktoerv2.AktoerV2;
import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
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

	private final AktoerV2 aktoerV2;
	private final ArkiverDokumentproduksjon arkiverDokumentproduksjon;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final BestemDistribusjonskanal bestemDistribusjonskanal;
	private final AdministrerForsendelse administrerForsendelse;
	private final PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper;

	@Inject
	public Qdist008Service(AktoerV2 aktoerV2,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   AdministrerForsendelse administrerForsendelse,
						   PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper) {
		this.aktoerV2 = aktoerV2;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseToRequestMapper = persisterForsendelseToRequestMapper;
	}

	@Handler
	public void distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);
		final HentIdentForAktoerIdResponseTo mottakerHentIdentForAktoerIdResponseTo = getFoedselsnummerIfIdentifikatorIsAktoerId(distribusjonbestilling
				.getMottaker());
		final HentIdentForAktoerIdResponseTo brukerHentIdentForAktoerIdResponseTo = getFoedselsnummerIfIdentifikatorIsAktoerId(distribusjonbestilling
				.getBruker());
		final DistribusjonsKanalCode distribusjonsKanal = bestemDistribusjonskanal.bestemKanal(
				mapDokDistKanalRequest(distribusjonbestilling, mottakerHentIdentForAktoerIdResponseTo, brukerHentIdentForAktoerIdResponseTo));
		exchange.setProperty(PROPERTY_DISTRIBUSJONSKANAL, distribusjonsKanal);

		final PersisterForsendelseRequestTo persisterForsendelseRequestTo = persisterForsendelseToRequestMapper
				.map(distribusjonbestilling, dokumenttypeInfoTo, mottakerHentIdentForAktoerIdResponseTo, distribusjonsKanal);

		if(!(distribusjonsKanal.equals(DistribusjonsKanalCode.INGEN_DISTRIBUSJON) || distribusjonsKanal.equals(DistribusjonsKanalCode.LOKAL_PRINT))){

			PersisterForsendelseResponseTo persisterForsendelseResponseTo = administrerForsendelse.persisterForsendelse(persisterForsendelseRequestTo);
			exchange.setProperty(PROPERTY_FORSENDELSE_ID, persisterForsendelseResponseTo.getForsendelseId());

			DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal().withForsendelseId(persisterForsendelseResponseTo.getForsendelseId());
			exchange.setProperty(PROPERTY_DISTRIBUSJONS_OBJEKT, distribuerTilKanal);
		}

		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);
		updateQdist008Metrics(persisterForsendelseRequestTo, distribusjonbestilling);
	}

	private DokumenttypeInfoTo getTittelFromDokkkatIfNotProvided(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		if (isEmpty(distribusjonbestilling.getForsendelseTittel())) {
			return dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(distribusjonbestilling));
		} else {
			return null;
		}
	}

	private HentIdentForAktoerIdResponseTo getFoedselsnummerIfIdentifikatorIsAktoerId(DistribuerForsendelseTo.AktoerTo aktoer) {
		if (aktoer.isIdentifikatorAktoerId()) {
			return aktoerV2.hentIdentForAktoerId(aktoer.getIdentifikator());
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
							.build(),
					SERVICE_ID);
		}
	}

	private String getIdentifikator(DistribuerForsendelseTo.AktoerTo aktoerTo, HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo) {
		if (aktoerTo.isIdentifikatorAktoerId()) {
			return hentIdentForAktoerIdResponseTo.getFoedselsnr();
		}
		return aktoerTo.getIdentifikator();
	}

	private DokDistKanalRequest mapDokDistKanalRequest(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo,
													   HentIdentForAktoerIdResponseTo mottakerHentIdentForAktoerIdResponseTo,
													   HentIdentForAktoerIdResponseTo brukerHentIdentForAktoerIdResponseTo) {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(getDokumenttypeIdHoveddokument(distribusjonbestillingTo))
				.mottakerId(getIdentifikator(distribusjonbestillingTo.getMottaker(), mottakerHentIdentForAktoerIdResponseTo))
				.mottakerType(distribusjonbestillingTo.getMottaker().getAktoerType().name())
				.brukerId(getIdentifikator(distribusjonbestillingTo.getBruker(), brukerHentIdentForAktoerIdResponseTo))
				.erArkivert(distribusjonbestillingTo.getArkivInformasjon() != null)
				.build();
	}
}
