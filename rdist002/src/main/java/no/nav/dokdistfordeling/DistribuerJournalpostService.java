package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostRequest;
import no.nav.dokdistfordeling.consumer.regoppslag.Regoppslag;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.Adresse;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper;
import no.nav.dokdistfordeling.map.MottakerMapper;
import no.nav.dokdistfordeling.map.RegoppslagAdresseMapper;
import no.nav.dokdistfordeling.util.SafeLoggingUtil;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.DOKDISTBESTILLINGS_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistfordeling.validate.AdresseValidator.validateAdresse;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.validateJournalpostAndDokumenter;
import static no.nav.dokdistfordeling.validate.TvingKanalValidator.validateTvingKanal;
import static org.apache.commons.lang3.StringUtils.capitalize;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final Regoppslag regoppslag;
	private final BestemDistribusjonskanalService bestemDistribusjonskanalService;
	private final JournalpostApi journalpostApi;

	public DistribuerJournalpostService(DistribuerForsendelseProducer distribuerForsendelseProducer,
										Regoppslag regoppslag,
										BestemDistribusjonskanalService bestemDistribusjonskanalService,
										JournalpostApi journalpostApi) {
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.regoppslag = regoppslag;
		this.bestemDistribusjonskanalService = bestemDistribusjonskanalService;
		this.journalpostApi = journalpostApi;
	}

	public String distribuerForsendelse(DistribuerJournalpost distribuerJournalpost,
										Journalpost journalpost) {

		final String bestillingsId = UUID.randomUUID().toString();

		validateJournalpostAndDokumenter(journalpost);
		validateTvingKanal(distribuerJournalpost, journalpost);

		Aktoer mottaker = MottakerMapper.map(journalpost.getAvsenderMottaker());
		DistribusjonKanalCode distribusjonKanalCode = bestemDistribusjonskanal(distribuerJournalpost, journalpost);
		Adresse adresse = utledAdresse(distribuerJournalpost, distribusjonKanalCode, journalpost);

		validateAdresse(adresse, mottaker);

		distribuerForsendelse(distribuerJournalpost, adresse, bestillingsId, journalpost, mottaker, distribusjonKanalCode);
		oppdaterJournalpostTilleggsopplysninger(distribuerJournalpost.journalpostId(), bestillingsId);

		return bestillingsId;
	}

	private DistribusjonKanalCode bestemDistribusjonskanal(DistribuerJournalpost distribuerJournalpost,
														   Journalpost journalpost) {
		boolean harAdresse = distribuerJournalpost.adresse() != null;
		boolean tvingSentralPrint = distribuerJournalpost.tvingSentralPrint();
		TvingKanal tvingKanal = distribuerJournalpost.tvingKanal();

		if (tvingSentralPrint) {
			log.info("tvingSentralPrint er satt til true i input. Forsendelsen vil bli sendt til print.");
			return PRINT;
		}

		if (tvingKanal != null) {
			log.info("tvingKanal er satt til {} i input. Forsendelsen vil bli sendt til {}.", tvingKanal, capitalize(tvingKanal.name()));

			return switch (tvingKanal) {
				case TvingKanal.PRINT -> PRINT;
				case TvingKanal.TRYGDERETTEN -> TRYGDERETTEN;
			};
		}

		return bestemDistribusjonskanalService.bestemDistribusjonskanal(journalpost, harAdresse);
	}

	private void distribuerForsendelse(DistribuerJournalpost distribuerJournalpost,
									   Adresse adresse,
									   String bestillingsId,
									   Journalpost journalpost,
									   Aktoer mottaker,
									   DistribusjonKanalCode distribusjonKanalCode) {

		HentDokumenterFraJoark hentDokumenterFraJoark = HentDokumenterFraJoarkMapper.map(
				distribuerJournalpost,
				adresse,
				journalpost,
				mottaker,
				bestillingsId,
				distribusjonKanalCode);

		distribuerForsendelseProducer.produce(
				hentDokumenterFraJoark,
				bestillingsId,
				distribuerJournalpost.journalpostId());
	}

	private void oppdaterJournalpostTilleggsopplysninger(String journalpostId,
														 String bestillingsId) {

		var request = OppdaterJournalpostRequest.builder()
				.tilleggsopplysninger(List.of(
						OppdaterJournalpostRequest.Tilleggsopplysning.builder()
								.nokkel(DOKDISTBESTILLINGS_ID)
								.verdi(bestillingsId)
								.build()))
				.build();

		journalpostApi.oppdaterJournalpost(journalpostId, request);
	}

	private Adresse utledAdresse(DistribuerJournalpost distribuerJournalpost,
								 DistribusjonKanalCode distribusjonKanalCode,
								 Journalpost journalpost) {

		if (distribuerJournalpost.adresse() == null && PRINT.equals(distribusjonKanalCode)) {
			return hentAdresse(distribuerJournalpost, journalpost);
		}

		return distribuerJournalpost.adresse();
	}

	private Adresse hentAdresse(DistribuerJournalpost distribuerJournalpost,
								Journalpost journalpost) {

		log.info("rdist002 request mangler adresse. Henter adresse fra regoppslag for mottaker på journalpostId={}",
				SafeLoggingUtil.removeUnsafeChars(distribuerJournalpost.journalpostId()));

		var avsenderMottaker = journalpost.getAvsenderMottaker();
		var tema = journalpost.getTema();

		return switch (avsenderMottaker.getType()) {
			case FNR ->
					RegoppslagAdresseMapper.map(regoppslag.hentPersonAdresse(avsenderMottaker.getId(), tema));
			case ORGNR ->
					RegoppslagAdresseMapper.map(regoppslag.hentOrganisasjonAdresse(avsenderMottaker.getId()));
			default ->
					throw new ValidationException("Journalpost.avsenderMottaker.idType må være FNR eller ORGNR hvis adresse ikke oppgis i request.");
		};
	}

}
