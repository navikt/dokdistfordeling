package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostRequest;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostResponse;
import no.nav.dokdistfordeling.consumer.regoppslag.Regoppslag;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateAdresse;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateDistribuerJournalpostRequest;
import static no.nav.dokdistfordeling.Rdist002ValidationUtil.validateJournalpostAndDokumenter;
import static no.nav.dokdistfordeling.constants.Constants.DOKDISTBESTILLINGS_ID;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.UKJENT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static org.apache.logging.log4j.util.Strings.isEmpty;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper;
	private final Regoppslag regoppslag;
	private final RegoppslagAdresseMapper regoppslagAdresseMapper;
	private final BestemDistribusjonskanalService bestemDistribusjonskanalService;
	private final JournalpostApi journalpostApi;

	public DistribuerJournalpostService(
			DistribuerForsendelseProducer distribuerForsendelseProducer,
			Regoppslag regoppslag,
			RegoppslagAdresseMapper regoppslagAdresseMapper,
			BestemDistribusjonskanalService bestemDistribusjonskanalService,
			JournalpostApi journalpostApi) {
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.regoppslagAdresseMapper = regoppslagAdresseMapper;
		this.hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();
		this.regoppslag = regoppslag;
		this.bestemDistribusjonskanalService = bestemDistribusjonskanalService;
		this.journalpostApi = journalpostApi;
	}

	public String distribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo, Journalpost journalpost) {
		final String bestillingsId = UUID.randomUUID().toString();
		DistribuerJournalpostRequestTo trimmetDistribuerJournalpostRequestTo = trimAdresse(distribuerJournalpostRequestTo);
		validateDistribuerJournalpostRequest(trimmetDistribuerJournalpostRequestTo);

		validateJournalpostAndDokumenter(journalpost);

		Aktoer mottaker = mapMottaker(journalpost.getAvsenderMottaker());

		DistribusjonKanalCode distribusjonKanalCode = bestemDistribusjonskanal(trimmetDistribuerJournalpostRequestTo, journalpost);

		DistribuerJournalpostRequestTo distribuerRequest = isNull(trimmetDistribuerJournalpostRequestTo.getAdresse()) && PRINT.equals(distribusjonKanalCode) ?
				hentDistribuerAdresseFraRegoppslag(trimmetDistribuerJournalpostRequestTo, journalpost) : trimmetDistribuerJournalpostRequestTo;

		if (distribuerRequest.getAdresse() != null) {
			validateAdresse(distribuerRequest.getAdresse(), mottaker);
		}

		hentDokumentOgDistribuerForsendelse(distribuerRequest, bestillingsId, journalpost, mottaker, distribusjonKanalCode);

		OppdaterJournalpostResponse oppdaterJournalpostResponse = oppdaterJournalpostTilleggsopplysninger(trimmetDistribuerJournalpostRequestTo.getJournalpostId(), bestillingsId);

		log.info("Oppdatert journalpost med journalpostId={} tilleggsopplysninger med nøkkel={} og verdi={}", oppdaterJournalpostResponse.getJournalpostId(), DOKDISTBESTILLINGS_ID, bestillingsId);

		return bestillingsId;
	}

	private DistribusjonKanalCode bestemDistribusjonskanal(DistribuerJournalpostRequestTo trimmetDistribuerJournalpostRequestTo, Journalpost journalpost){
		boolean harAdresse = nonNull(trimmetDistribuerJournalpostRequestTo.getAdresse());
		if(trimmetDistribuerJournalpostRequestTo.isTvingSentralPrint()){
			log.info("tvingSentralPrint er satt til true i input. Forsendelsen vil bli sendt til print.");
			return PRINT;
		} else {
			return bestemDistribusjonskanalService.bestemDistribusjonskanal(journalpost, harAdresse);
		}
	}

	private void hentDokumentOgDistribuerForsendelse(final DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
													 final String bestillingsId,
													 final Journalpost journalpost,
													 final Aktoer mottaker, DistribusjonKanalCode distribusjonKanalCode) {
		final HentDokumenterFraJoark hentDokumenterFraJoark = hentDokumenterFraJoarkMapper.map(distribuerJournalpostRequestTo, journalpost, mottaker, bestillingsId, distribusjonKanalCode);
		distribuerForsendelseProducer.produce(hentDokumenterFraJoark,
				bestillingsId,
				distribuerJournalpostRequestTo.getJournalpostId());
	}

	private OppdaterJournalpostResponse oppdaterJournalpostTilleggsopplysninger(String journalpostId, String bestillingsId) {
		return journalpostApi.oppdaterJournalpost(journalpostId, OppdaterJournalpostRequest.builder()
				.tilleggsopplysninger(List.of(OppdaterJournalpostRequest.Tilleggsopplysning.builder()
						.nokkel(DOKDISTBESTILLINGS_ID)
						.verdi(bestillingsId)
						.build()))
				.build());
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
		return opprinneligAdresse != null && !opprinneligAdresse.trim().isEmpty() ? opprinneligAdresse.trim() : null;
	}

	private DistribuerJournalpostRequestTo hentDistribuerAdresseFraRegoppslag(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo,
																			  Journalpost journalpost) {
		log.info("rdist002 request mangler adresse. Henter adresse fra regoppslag for mottaker på journalpostId={}",
				distribuerJournalpostRequestTo.getJournalpostId());

		return distribuerJournalpostRequestTo.toBuilder()
				.adresse(hentAdresse(journalpost.getAvsenderMottaker(), journalpost.getTema()))
				.build();
	}

	private DistribuerJournalpostRequestTo.AdresseTo hentAdresse(Journalpost.AvsenderMottaker avsenderMottaker, String tema) {
		return switch (avsenderMottaker.getType()) {
			case FNR ->
					regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentPersonAdresse(avsenderMottaker.getId(), tema));
			case ORGNR ->
					regoppslagAdresseMapper.mapAdresseTo(regoppslag.hentOrganisasjonAdresse(avsenderMottaker.getId()));
			default ->
					throw new ValidationException("Journalpost.avsenderMottaker.idType må være FNR eller ORGNR hvis adresse ikke oppgis i request.");
		};
	}

	private Aktoer mapMottaker(Journalpost.AvsenderMottaker avsenderMottaker) {
		if (avsenderMottaker.getType() == null) {
			Samhandler samhandler = new Samhandler();
			samhandler.setNavn(avsenderMottaker.getNavn());
			samhandler.setSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()));
			samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
			return samhandler;
		} else {
			return switch (avsenderMottaker.getType()) {
				case FNR -> {
					Person person = new Person();
					person.setNavn(avsenderMottaker.getNavn());
					person.setPersonidentifikator(avsenderMottaker.getId());
					yield person;
				}
				case ORGNR -> {
					Organisasjon organisasjon = new Organisasjon();
					organisasjon.setNavn(avsenderMottaker.getNavn());
					organisasjon.setOrgnummer(avsenderMottaker.getId());
					yield organisasjon;
				}
				case HPRNR -> {
					Samhandler samhandler = new Samhandler();
					samhandler.setNavn(avsenderMottaker.getNavn());
					samhandler.setSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()));
					samhandler.setSamhandlerkategori(SamhandlerKategoriCode.HPR.name());
					yield samhandler;
				}
				case UTL_ORG -> {
					Samhandler samhandler = new Samhandler();
					samhandler.setNavn(avsenderMottaker.getNavn());
					samhandler.setSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()));
					samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UTL_ORG.name());
					yield samhandler;
				}
				case UKJENT -> {
					Samhandler samhandler = new Samhandler();
					samhandler.setNavn(avsenderMottaker.getNavn());
					samhandler.setSamhandleridentifikator(determineAvsenderMottakerId(avsenderMottaker.getId()));
					samhandler.setSamhandlerkategori(SamhandlerKategoriCode.UKJENT.name());
					yield samhandler;
				}
			};
		}
	}

	private String determineAvsenderMottakerId(String mottakerId) {
		return isEmpty(mottakerId) ? UKJENT.name() : mottakerId;
	}

}
