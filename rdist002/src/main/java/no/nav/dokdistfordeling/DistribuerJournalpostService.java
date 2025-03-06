package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostRequest;
import no.nav.dokdistfordeling.consumer.regoppslag.Regoppslag;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper;
import no.nav.dokdistfordeling.map.MottakerMapper;
import no.nav.dokdistfordeling.map.RegoppslagAdresseMapper;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.DOKDISTBESTILLINGS_ID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.validateJournalpostAndDokumenter;
import static no.nav.dokdistfordeling.validate.PostadresseValidator.validatePostdresse;
import static no.nav.dokdistfordeling.validate.TvingKanalValidator.validateTvingKanal;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final Regoppslag regoppslag;
	private final BestemDistribusjonskanalService bestemDistribusjonskanalService;
	private final JournalpostApi journalpostApi;
	private final PersonnummerService personnummerService;

	public DistribuerJournalpostService(DistribuerForsendelseProducer distribuerForsendelseProducer,
										Regoppslag regoppslag,
										BestemDistribusjonskanalService bestemDistribusjonskanalService,
										JournalpostApi journalpostApi,
										PersonnummerService personnummerService) {
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.regoppslag = regoppslag;
		this.bestemDistribusjonskanalService = bestemDistribusjonskanalService;
		this.journalpostApi = journalpostApi;
		this.personnummerService = personnummerService;
	}

	public String distribuerForsendelse(DistribuerJournalpost distribuerJournalpost,
										Journalpost journalpost) {

		final String bestillingsId = UUID.randomUUID().toString();

		validateJournalpostAndDokumenter(journalpost);
		validateTvingKanal(distribuerJournalpost, journalpost);

		Aktoer mottaker = MottakerMapper.map(journalpost.getAvsenderMottaker());

		String personnummer = personnummerService.utledPersonnummer(journalpost.getBruker(), distribuerJournalpost.harPostadresse());

		DistribusjonKanalCode distribusjonKanalCode = bestemDistribusjonskanalService.bestemDistribusjonskanal(distribuerJournalpost, journalpost, personnummer);

		Postadresse postadresse = utledPostadresse(distribuerJournalpost, distribusjonKanalCode, journalpost);

		validatePostdresse(postadresse, mottaker);

		distribuerForsendelse(distribuerJournalpost, postadresse, bestillingsId, journalpost, mottaker, distribusjonKanalCode);

		oppdaterJournalpostMedTilleggsopplysninger(distribuerJournalpost.journalpostId(), bestillingsId);

		return bestillingsId;
	}

	private void distribuerForsendelse(DistribuerJournalpost distribuerJournalpost,
									   Postadresse postadresse,
									   String bestillingsId,
									   Journalpost journalpost,
									   Aktoer mottaker,
									   DistribusjonKanalCode distribusjonKanalCode) {

		var hentDokumenterFraJoark = HentDokumenterFraJoarkMapper.map(
				distribuerJournalpost,
				postadresse,
				journalpost,
				mottaker,
				bestillingsId,
				distribusjonKanalCode);

		distribuerForsendelseProducer.produce(
				hentDokumenterFraJoark,
				bestillingsId,
				String.valueOf(distribuerJournalpost.journalpostId()));
	}

	private void oppdaterJournalpostMedTilleggsopplysninger(Long journalpostId,
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

	private Postadresse utledPostadresse(DistribuerJournalpost distribuerJournalpost,
										 DistribusjonKanalCode distribusjonKanalCode,
										 Journalpost journalpost) {

		if (!distribuerJournalpost.harPostadresse() && distribusjonKanalCode == PRINT) {
			return hentPostadresse(distribuerJournalpost, journalpost);
		}

		return distribuerJournalpost.postadresse();
	}

	private Postadresse hentPostadresse(DistribuerJournalpost distribuerJournalpost,
										Journalpost journalpost) {

		log.info("rdist002 request mangler postadresse. Henter postadresse fra regoppslag for mottaker på journalpostId={}", distribuerJournalpost.journalpostId());

		var avsenderMottaker = journalpost.getAvsenderMottaker();
		var tema = journalpost.getTema();

		return switch (avsenderMottaker.getType()) {
			case FNR ->
					RegoppslagAdresseMapper.map(regoppslag.hentPersonAdresse(avsenderMottaker.getId(), tema));
			case ORGNR ->
					RegoppslagAdresseMapper.map(regoppslag.hentOrganisasjonAdresse(avsenderMottaker.getId()));
			default ->
					throw new ValidationException("Journalpost.avsenderMottaker.idType må være FNR eller ORGNR hvis postadresse ikke oppgis i request.");
		};
	}

}
