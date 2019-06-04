package no.nav.dokdistfordeling;

import static no.nav.dokdistfordeling.constants.Constants.CALL_ID;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private SafJournalpostQueryService safJournalpostQueryService;
	private DistribuerForsendelseProducer distribuerForsendelseProducer;
	private DokumentkatalogAdmin dokumentkatalogAdmin;
	private HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper;
	private Rdist002ValidationUtil rdist002ValidationUtil;

	public DistribuerJournalpostService(SafJournalpostQueryService safJournalpostQueryService,
										DistribuerForsendelseProducer distribuerForsendelseProducer,
										DokumentkatalogAdmin dokumentkatalogAdmin) {
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();
		this.rdist002ValidationUtil = new Rdist002ValidationUtil();
	}

	public String distribuerForsendelse(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, String authorizationHeader) {
		String bestillingsId = createBestillingsIdAndStoreAsCallId();

		rdist002ValidationUtil.validateRequest(distribuerJournalpostRequestTo);

		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);
		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);

		Aktoer mottaker = mapMottaker(journalpost.getAvsenderMottaker());
		rdist002ValidationUtil.validateAdresse(distribuerJournalpostRequestTo.getAdresse(), mottaker);

		List<Journalpost.DokumentInfo> dokumenter = journalpost.getDokumenter();
		Journalpost.DokumentInfo hovedDokumentInfo = dokumenter.iterator().next();

		// brevkode for utgående dokumenter tilsvarer dokumenttypeid
		dokumentkatalogAdmin.getDokumenttypeInfo(hovedDokumentInfo.getBrevkode());

		distribuerForsendelseProducer.produce(hentDokumenterFraJoarkMapper.map(distribuerJournalpostRequestTo, journalpost, mottaker, bestillingsId),
				bestillingsId,
				distribuerJournalpostRequestTo.getJournalpostId());

		return bestillingsId;
	}

	private String createBestillingsIdAndStoreAsCallId() {
		String id = UUID.randomUUID().toString();
		MDC.put(CALL_ID, id);
		return id;
	}

	private Aktoer mapMottaker(Journalpost.AvsenderMottaker avsenderMottaker) {
		Aktoer output;
		switch (avsenderMottaker.getType()) {
			case FNR:
				output = new Person()
						.withNavn(avsenderMottaker.getNavn())
						.withPersonidentifikator(avsenderMottaker.getId());
				break;
			case ORGNR:
				output = new Organisasjon()
						.withNavn(avsenderMottaker.getNavn())
						.withOrgnummer(avsenderMottaker.getId());
				break;
			case HPRNR:
				output = new Samhandler()
						.withNavn(avsenderMottaker.getNavn())
						.withSamhandleridentifikator(avsenderMottaker.getId())
						.withSamhandlerkategori(avsenderMottaker.getType().name());
				break;
			case UTL_ORG:
				output = new Samhandler()
						.withNavn(avsenderMottaker.getNavn())
						.withSamhandleridentifikator(avsenderMottaker.getId())
						.withSamhandlerkategori(avsenderMottaker.getType().name());
				break;
			default:
				output = null;
				break;
		}
		return output;
	}
}
