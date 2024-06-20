package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterDistribusjonsinfoTo;
import no.nav.dokdistfordeling.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistfordeling.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistfordeling.consumer.rdist001.AdministrerForsendelse;
import no.nav.dokdistfordeling.consumer.rdist001.OpprettForsendelseRequestTo;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.AktoerTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist008.domain.OpprettForsendelseToRequestMapper;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.out.DistribuerTilKanal;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.springframework.stereotype.Service;

import static no.nav.dokdistfordeling.kodeverk.ArkivSystemCode.JOARK;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_DISTRIBUSJONSKANAL;
import static no.nav.dokdistfordeling.qdist008.Qdist008Route.PROPERTY_FORSENDELSE_ID;
import static no.nav.dokdistfordeling.util.Qdist008Util.getDokumenttypeIdHoveddokument;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class Qdist008Service {

	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final DokmetConsumer dokmetConsumer;
	private final AdministrerForsendelse administrerForsendelse;
	private final OpprettForsendelseToRequestMapper opprettForsendelseToRequestMapper;
	private final JournalpostApi journalpostApi;

	public Qdist008Service(PdlGraphQLConsumer pdlGraphQLConsumer,
						   DokmetConsumer dokmetConsumer,
						   AdministrerForsendelse administrerForsendelse,
						   OpprettForsendelseToRequestMapper opprettForsendelseToRequestMapper,
						   JournalpostApi journalpostApi) {
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.dokmetConsumer = dokmetConsumer;
		this.administrerForsendelse = administrerForsendelse;
		this.opprettForsendelseToRequestMapper = opprettForsendelseToRequestMapper;
		this.journalpostApi = journalpostApi;
	}

	@Handler
	public DistribuerTilKanal distribuerForsendelseService(DistribuerForsendelseTo distribuerForsendelseTo, Exchange exchange) {
		DistribusjonbestillingTo distribusjonbestilling = distribuerForsendelseTo.getDistribusjonbestilling();

		final String forsendelseTittel = getForsendelseTittel(distribusjonbestilling);
		final String mottakerFnr = getFnr(distribusjonbestilling.getMottaker());

		final DistribusjonKanalCode distribusjonsKanal = DistribusjonKanalCode.valueOf(distribusjonbestilling.getDistribusjonKanal());
		exchange.setProperty(PROPERTY_DISTRIBUSJONSKANAL, distribusjonbestilling.getDistribusjonKanal());

		DistribuerTilKanal distribuerTilKanal = new DistribuerTilKanal();

		if (!(INGEN_DISTRIBUSJON.equals(distribusjonsKanal) || LOKAL_PRINT.equals(distribusjonsKanal))) {

			final OpprettForsendelseRequestTo opprettForsendelseRequestTo = opprettForsendelseToRequestMapper
					.map(distribusjonbestilling, forsendelseTittel, mottakerFnr, distribusjonsKanal);

			String forsendelseId = administrerForsendelse.opprettForsendelse(opprettForsendelseRequestTo);
			exchange.setProperty(PROPERTY_FORSENDELSE_ID, forsendelseId);

			distribuerTilKanal.setForsendelseId(forsendelseId);
		}

		updateArkivIfArkivsystemIsJoark(distribusjonbestilling, distribusjonsKanal);

		return distribuerTilKanal;
	}

	private String getForsendelseTittel(DistribusjonbestillingTo distribusjonbestilling) {
		if (isBlank(distribusjonbestilling.getForsendelseTittel())) {
			var dokumenttypeId = getDokumenttypeIdHoveddokument(distribusjonbestilling);
			var dokumenttypeInfo = dokmetConsumer.getDokumenttypeInfo(dokumenttypeId);

			return dokumenttypeInfo.dokumentTittel();
		}

		return distribusjonbestilling.getForsendelseTittel();
	}

	private String getFnr(AktoerTo aktoer) {
		if (aktoer.isIdentifikatorAktoerId()) {
			return pdlGraphQLConsumer.hentFolkeregisteridentForAktoerId(aktoer.getIdentifikator());
		} else {
			return aktoer.getIdentifikator();
		}
	}

	private void updateArkivIfArkivsystemIsJoark(DistribusjonbestillingTo distribusjonbestilling, DistribusjonKanalCode distribusjonsKanal) {
		final DistribuerForsendelseTo.ArkivInformasjonTo arkivInformasjon = distribusjonbestilling.getArkivInformasjon();
		if (arkivInformasjon != null && arkivInformasjon.getArkivSystem().equals(JOARK)) {

			journalpostApi.oppdaterDistribusjonsinfo(
					arkivInformasjon.getArkivId(),
					OppdaterDistribusjonsinfoTo.builder()
							.settStatusEkspedert(false)
							.utsendingsKanal(distribusjonsKanal.getJoarkUtsendingsKanal())
							.build());
		}
	}

}
