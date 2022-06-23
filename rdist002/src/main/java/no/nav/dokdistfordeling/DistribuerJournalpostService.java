package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostRequest;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostResponse;
import no.nav.dokdistfordeling.consumer.regoppslag.Regoppslag;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistfordeling.constants.Constants.DOKDISTBESTILLINGS_ID;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.UKJENT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper;
	private final Rdist002ValidationUtil rdist002ValidationUtil;
	private final Regoppslag regoppslag;
	private final RegoppslagAdresseMapper regoppslagAdresseMapper;
	private final HentBestemDokdistKanalService hentBestemDokdistKanal;
	private final JournalpostApi journalpostApi;

	public DistribuerJournalpostService(
										DistribuerForsendelseProducer distribuerForsendelseProducer,
										Regoppslag regoppslag, RegoppslagAdresseMapper regoppslagAdresseMapper,
										HentBestemDokdistKanalService hentBestemDokdistKanal, JournalpostApi journalpostApi) {
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.regoppslagAdresseMapper = regoppslagAdresseMapper;
		this.hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();
		this.rdist002ValidationUtil = new Rdist002ValidationUtil();
		this.regoppslag = regoppslag;
		this.hentBestemDokdistKanal = hentBestemDokdistKanal;
		this.journalpostApi = journalpostApi;
	}

	public String distribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost) {
		final String bestillingsId = UUID.randomUUID().toString();
		DistribuerJournalpostRequestTo trimmetDistribuerJournalpostRequestTo = trimAdresse(distribuerJournalpostRequestTo);
		rdist002ValidationUtil.validateRequest(trimmetDistribuerJournalpostRequestTo);

		rdist002ValidationUtil.validateJournalpostAndDokumenter(journalpost);

		Aktoer mottaker = mapMottaker(journalpost.getAvsenderMottaker());
		boolean harAdresse = nonNull(trimmetDistribuerJournalpostRequestTo.getAdresse());
		DistribusjonsKanalCode distribusjonsKanalCode = hentBestemDokdistKanal.bestemDistribusjonskanal(journalpost, harAdresse);

		DistribuerJournalpostRequestTo distribuerRequest = isNull(trimmetDistribuerJournalpostRequestTo.getAdresse()) && PRINT.equals(distribusjonsKanalCode) ?
				hentDistribuerAdresseFraRegoppslag(trimmetDistribuerJournalpostRequestTo, journalpost) : trimmetDistribuerJournalpostRequestTo;

		OppdaterJournalpostResponse oppdaterJournalpostResponse = journalpostApi.oppdaterJournalpost(trimmetDistribuerJournalpostRequestTo.getJournalpostId(), OppdaterJournalpostRequest.builder()
				.tilleggsopplysninger(Arrays.asList(OppdaterJournalpostRequest.Tilleggsopplysning.builder()
						.nokkel(DOKDISTBESTILLINGS_ID)
						.verdi(bestillingsId)
						.build()))
				.build());
		log.info("Oppdatert journalpost med journalpostId={} tilleggsopplysninger med nøkkel={} og verdi={}", oppdaterJournalpostResponse.getJournalpostId(), DOKDISTBESTILLINGS_ID, bestillingsId);

		return doDistribuerForsendelse(distribuerRequest, bestillingsId, journalpost, mottaker, distribusjonsKanalCode);
	}

	private DistribuerJournalpostRequestTo trimAdresse(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		if (distribuerJournalpostRequestTo.getAdresse() != null) {
			DistribuerJournalpostRequestTo.AdresseTo opprinneligAdresse = distribuerJournalpostRequestTo.getAdresse();
			DistribuerJournalpostRequestTo.AdresseTo adresseTo =
					DistribuerJournalpostRequestTo.AdresseTo.builder()
							.adresselinje1(trimAdresselinje(opprinneligAdresse.getAdresselinje1()))
							.adresselinje2(trimAdresselinje(opprinneligAdresse.getAdresselinje2()))
							.adresselinje3(trimAdresselinje(opprinneligAdresse.getAdresselinje3()))
							.postnummer(opprinneligAdresse.getPostnummer())
							.poststed(opprinneligAdresse.getPoststed())
							.land(opprinneligAdresse.getLand())
							.build();
			distribuerJournalpostRequestTo.toBuilder().adresse(adresseTo).build();
		}
		return distribuerJournalpostRequestTo;
	}

	private String trimAdresselinje(String opprinneligAdresse) {
		return opprinneligAdresse != null && opprinneligAdresse.trim().length() > 0 ? opprinneligAdresse.trim() : null;
	}

	private DistribuerJournalpostRequestTo hentDistribuerAdresseFraRegoppslag(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																			  Journalpost journalpost) {
		log.info("rdist002 request mangler adresse. Henter adresse fra regoppslag for mottaker på journalpostId={}",
				distribuerJournalpostRequestTo.getJournalpostId());

		return distribuerJournalpostRequestTo.toBuilder()
				.adresse(hentAdresse(journalpost.getAvsenderMottaker(), journalpost.getTema()))
				.build();
	}

	private String doDistribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
										   final String bestillingsId,
										   final Journalpost journalpost,
										   final Aktoer mottaker, DistribusjonsKanalCode distribusjonsKanalCode) {

		if (PRINT.name().equals(distribusjonsKanalCode.name())) {
			rdist002ValidationUtil.validateAdresse(distribuerJournalpostRequestTo.getAdresse(), mottaker);
		}

		final HentDokumenterFraJoark hentDokumenterFraJoark = hentDokumenterFraJoarkMapper.map(distribuerJournalpostRequestTo, journalpost, mottaker, bestillingsId, distribusjonsKanalCode);
		distribuerForsendelseProducer.produce(hentDokumenterFraJoark,
				bestillingsId,
				distribuerJournalpostRequestTo.getJournalpostId());

		return bestillingsId;
	}

	private DistribuerJournalpostRequestTo.AdresseTo hentAdresse(Journalpost.AvsenderMottaker avsenderMottaker, String tema) {
		switch (avsenderMottaker.getType()) {
			case FNR:
				return regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentPersonAdresse(avsenderMottaker.getId(), tema));
			case ORGNR:
				return regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentOrganisasjonAdresse(avsenderMottaker.getId()));
			default:
				throw new ValidationException("Journalpost.avsenderMottaker.idType må være FNR eller ORGNR hvis adresse ikke oppgis i request.");
		}
	}

	private Aktoer mapMottaker(Journalpost.AvsenderMottaker avsenderMottaker) {
		Aktoer output;
		if(avsenderMottaker.getType() == null || isEmpty(avsenderMottaker.getType().name())){
			output = new Samhandler()
					.withNavn(avsenderMottaker.getNavn())
					.withSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()))
					.withSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
		}
		else {
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
							.withSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()))
							.withSamhandlerkategori(SamhandlerKategoriCode.HPR.name());
					break;
				case UTL_ORG:
					output = new Samhandler()
							.withNavn(avsenderMottaker.getNavn())
							.withSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()))
							.withSamhandlerkategori(SamhandlerKategoriCode.UTL_ORG.name());
					break;
				case UKJENT:
					output = new Samhandler()
							.withNavn(avsenderMottaker.getNavn())
							.withSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()))
							.withSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
					break;
				default:
					output = null;
					break;
			}
		}
		return output;
	}

	private String determineAvsenderMottakerId(String mottakerId){
		return isEmpty(mottakerId) ? UKJENT.name() : mottakerId;
	}

}
