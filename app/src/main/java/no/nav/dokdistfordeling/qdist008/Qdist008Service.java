package no.nav.dokdistfordeling.qdist008;

import static org.springframework.util.StringUtils.isEmpty;

import no.nav.dokdistfordeling.consumer.aktoerv2.Aktoer;
import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.bestemdistribusjonskanal.BestemDistribusjonskanal;
import no.nav.dokdistfordeling.consumer.dokdist.rdist001.Forsendelse;
import no.nav.dokdistfordeling.consumer.dokdist.rdist001.ForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.dokdist.rdist001.ForsendelseToRequestMapper;
import no.nav.dokdistfordeling.consumer.tjoark110.ArkiverDokumentproduksjon;
import no.nav.dokdistfordeling.consumer.tjoark110.SettJournalpostAttributterRequestTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.stream.Collectors;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Service
public class Qdist008Service {

	private final Aktoer aktoer;
	private final ArkiverDokumentproduksjon arkiverDokumentproduksjon;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final BestemDistribusjonskanal bestemDistribusjonskanal;
	private final Forsendelse forsendelse;
	private final ForsendelseToRequestMapper forsendelseToRequestMapper;

	@Inject
	public Qdist008Service(Aktoer aktoer,
						   ArkiverDokumentproduksjon arkiverDokumentproduksjon,
						   DokumentkatalogAdmin dokumentkatalogAdmin,
						   BestemDistribusjonskanal bestemDistribusjonskanal,
						   Forsendelse forsendelse,
						   ForsendelseToRequestMapper forsendelseToRequestMapper) {
		this.aktoer = aktoer;
		this.arkiverDokumentproduksjon = arkiverDokumentproduksjon;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.bestemDistribusjonskanal = bestemDistribusjonskanal;
		this.forsendelse = forsendelse;
		this.forsendelseToRequestMapper = forsendelseToRequestMapper;
	}

	@Handler
	public DistribuerForsendelseTilSentralPrint distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo) {
		DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final DokumenttypeInfoTo dokumenttypeInfoTo = getTittelFromDokkkatIfNotProvided(distribusjonbestilling);
		final HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo = getFoedselsnummerIfMottakerIdentifikatorIsAktoerId(distribusjonbestilling
				.getMottaker());
		final DistribusjonsKanalCode distribusjonsKanal = bestemDistribusjonskanal.bestemKanal();
		ForsendelseResponseTo forsendelseResponseTo = forsendelse.persisterForsendelse(forsendelseToRequestMapper.map(distribusjonbestilling, dokumenttypeInfoTo, hentIdentForAktoerIdResponseTo, distribusjonsKanal));
		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);

		return new DistribuerForsendelseTilSentralPrint(forsendelseResponseTo.getForsendelseId());
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

}
