package no.nav.dokdistfordeling.qdist008;

import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.SERVICE_ID;
import static no.nav.dokdistfordeling.qdist008.metrics.MetricUpdater.updateQdist008Metrics;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;
import static org.springframework.util.StringUtils.isEmpty;

import no.nav.dokdistfordeling.consumer.aktoerv2.AktoerConsumerInterface;
import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDistribusjonskanal;

import no.nav.dokdistfordeling.consumer.tjoark110.ArkiverDokumentproduksjon;
import no.nav.dokdistfordeling.consumer.tjoark110.SettJournalpostAttributterRequestTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.rdist001.PersisterForsendelseRequestTo;
import no.nav.dokdistfordeling.rdist001.PersisterForsendelseResponseTo;
import no.nav.dokdistfordeling.rdist001.PersisterForsendelseToRequestMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist008Service {

	private final AktoerConsumerInterface aktoerConsumerInterface;
	private final ArkiverDokumentproduksjon arkiverDokumentproduksjon;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final BestemDistribusjonskanal bestemDistribusjonskanal;
	private final AdministrerForsendelse administrerForsendelse;
	private final PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper;

	@Inject
	public Qdist008Service(AktoerConsumerInterface aktoerConsumerInterface,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   AdministrerForsendelse administrerForsendelse,
						   PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper) {
		this.aktoerConsumerInterface = aktoerConsumerInterface;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.administrerForsendelse = administrerForsendelse;
		this.persisterForsendelseToRequestMapper = persisterForsendelseToRequestMapper;
	}

	@Handler
	public DistribuerForsendelseTilSentralPrint distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);

		final HentIdentForAktoerIdResponseTo mottakerHentIdentForAktoerIdResponseTo = getFoedselsnummerIfAktoerIdentifikatorIsAktoerId(distribusjonbestilling.getMottaker());
		final HentIdentForAktoerIdResponseTo brukerHentIdentForAktoerIdResponseTo = getFoedselsnummerIfAktoerIdentifikatorIsAktoerId(distribusjonbestilling.getBruker());

		final DistribusjonsKanalCode distribusjonsKanal = bestemDistribusjonskanal.bestemKanal(
				useFoedselsnrIfPerson(distribusjonbestilling.getMottaker(), mottakerHentIdentForAktoerIdResponseTo),
				getDokumenttypeIdHoveddokument(distribusjonbestilling),
				distribusjonbestilling.getMottaker().getAktoerType(),
				useFoedselsnrIfPerson(distribusjonbestilling.getBruker(), brukerHentIdentForAktoerIdResponseTo));

		final PersisterForsendelseRequestTo persisterForsendelseRequestTo = persisterForsendelseToRequestMapper
				.map(distribusjonbestilling, dokumenttypeInfoTo, mottakerHentIdentForAktoerIdResponseTo, distribusjonsKanal);


		PersisterForsendelseResponseTo persisterForsendelseResponseTo = administrerForsendelse.persisterForsendelse(persisterForsendelseRequestTo);
		exchange.setProperty(PROPERTY_FORSENDELSE_ID, persisterForsendelseResponseTo.getForsendelseId());

		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);
		updateQdist008Metrics(persisterForsendelseRequestTo, distribusjonbestilling);

		return new DistribuerForsendelseTilSentralPrint(persisterForsendelseResponseTo.getForsendelseId());
	}

	private DokumenttypeInfoTo getTittelFromDokkkatIfNotProvided(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling) {
		if (isEmpty(distribusjonbestilling.getForsendelseTittel())) {
			return dokumentkatalogAdmin.getDokumenttypeInfo(getDokumenttypeIdHoveddokument(distribusjonbestilling));
		} else {
			return null;
		}
	}

	private HentIdentForAktoerIdResponseTo getFoedselsnummerIfAktoerIdentifikatorIsAktoerId(DistribuerForsendelseTo.AktoerTo aktoer) {
		if (aktoer.isIdentifikatorAktoerId()) {
			return aktoerConsumerInterface.hentIdentForAktoerId(aktoer.getIdentifikator());
		} else {
			return HentIdentForAktoerIdResponseTo.builder().foedselsnr(aktoer.getIdentifikator()).build();
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

	private String useFoedselsnrIfPerson(DistribuerForsendelseTo.AktoerTo mottakerTo, HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo) {
		if (AktoerTypeCode.PERSON.equals(mottakerTo.getAktoerType()) && hentIdentForAktoerIdResponseTo != null) {
			return hentIdentForAktoerIdResponseTo.getFoedselsnr();
		}
		return mottakerTo.getIdentifikator();
	}
}
