package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.regoppslag.Regoppslag;
import no.nav.dokdistfordeling.consumer.regoppslag.to.HentMottakerOgAdresseResponseTo;
import no.nav.dokdistfordeling.consumer.saf.SafJournalpostQueryService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.tkat020.DokumentkatalogAdmin;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final SafJournalpostQueryService safJournalpostQueryService;
	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final DokumentkatalogAdmin dokumentkatalogAdmin;
	private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper;
	private final Rdist002ValidationUtil rdist002ValidationUtil;
	private final Regoppslag regoppslag;
	private final RegoppslagAdresseMapper regoppslagAdresseMapper;

	public DistribuerJournalpostService(SafJournalpostQueryService safJournalpostQueryService,
										DistribuerForsendelseProducer distribuerForsendelseProducer,
										DokumentkatalogAdmin dokumentkatalogAdmin, Regoppslag regoppslag,
										RegoppslagAdresseMapper regoppslagAdresseMapper) {
		this.safJournalpostQueryService = safJournalpostQueryService;
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.dokumentkatalogAdmin = dokumentkatalogAdmin;
		this.regoppslagAdresseMapper = regoppslagAdresseMapper;
		this.hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();
		this.rdist002ValidationUtil = new Rdist002ValidationUtil();
		this.regoppslag = regoppslag;
	}

	public String distribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
										final String authorizationHeader) {
		final String bestillingsId = UUID.randomUUID().toString();

		rdist002ValidationUtil.validateRequest(distribuerJournalpostRequestTo);

		Journalpost journalpost = safJournalpostQueryService.hentJournalpost(distribuerJournalpostRequestTo.getJournalpostId(), authorizationHeader);
		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);

		Aktoer mottaker = mapMottaker(journalpost.getAvsenderMottaker());
		if(distribuerJournalpostRequestTo.getAdresse() == null) {
			log.info("rdist002 request mangler adresse. Henter adresse fra regoppslag for mottaker på journalpostId={}.", distribuerJournalpostRequestTo.getJournalpostId());
			DistribuerJournalpostRequestTo.AdresseTo regoppslagAdresse = hentAdresse(journalpost.getAvsenderMottaker());
			return doDistribuerForsendelse(distribuerJournalpostRequestTo.toBuilder()
					.adresse(regoppslagAdresse)
					.build(), bestillingsId, journalpost, mottaker);
		} else {
			return doDistribuerForsendelse(distribuerJournalpostRequestTo, bestillingsId, journalpost, mottaker);
		}
	}

	private String doDistribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
										   final String bestillingsId,
										   final Journalpost journalpost,
										   final Aktoer mottaker) {
		rdist002ValidationUtil.validateAdresse(distribuerJournalpostRequestTo.getAdresse(), mottaker);

		final HentDokumenterFraJoark hentDokumenterFraJoark = hentDokumenterFraJoarkMapper.map(distribuerJournalpostRequestTo, journalpost, mottaker, bestillingsId);
		distribuerForsendelseProducer.produce(hentDokumenterFraJoark,
				bestillingsId,
				distribuerJournalpostRequestTo.getJournalpostId());

		return bestillingsId;
	}

	private DistribuerJournalpostRequestTo.AdresseTo hentAdresse(Journalpost.AvsenderMottaker avsenderMottaker) {
		switch (avsenderMottaker.getType()) {
			case FNR:
				return regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentPersonAdresse(avsenderMottaker.getId()));
			case ORGNR:
				return regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentOrganisasjonAdresse(avsenderMottaker.getId()));
			default:
				throw new ValidationException("Journalpost.avsenderMottaker.idType må være FNR eller ORGNR hvis adresse ikke oppgis i request.");
		}
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
						.withSamhandlerkategori(SamhandlerKategoriCode.HPR.name());
				break;
			case UTL_ORG:
				output = new Samhandler()
						.withNavn(avsenderMottaker.getNavn())
						.withSamhandleridentifikator(avsenderMottaker.getId())
						.withSamhandlerkategori(SamhandlerKategoriCode.UTL_ORG.name());
				break;
			case UKJENT:
				output = new Samhandler()
						.withNavn(avsenderMottaker.getNavn())
						.withSamhandleridentifikator(avsenderMottaker.getId())
						.withSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
				break;
			default:
				output = null;
				break;
		}
		return output;
	}
}
