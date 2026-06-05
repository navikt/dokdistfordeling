package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokarkiv.OppdaterJournalpostRequest;
import no.nav.dokdistfordeling.consumer.dokdistadmin.DokdistadminConsumer;
import no.nav.dokdistfordeling.consumer.dokdistadmin.FinnForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.dokdistadmin.HentForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.regoppslag.PostadresseService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostIdempotencyHandler;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostInfoResponse;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.exception.functional.JournalpostErAlleredeDistribuertException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper;
import no.nav.dokdistfordeling.map.MottakerMapper;
import no.nav.dokdistfordeling.map.RegoppslagAdresseMapper;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static no.nav.dokdistfordeling.constants.Constants.DOKDISTBESTILLINGS_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.validate.JournalpostValidator.validateJournalpostAndDokumenter;
import static no.nav.dokdistfordeling.validate.PostadresseValidator.validatePostadresse;
import static no.nav.dokdistfordeling.validate.TvingKanalValidator.validateTvingKanal;

@Component
@Slf4j
public class DistribuerJournalpostService {

	private final DistribuerForsendelseProducer distribuerForsendelseProducer;
	private final DistribuerJournalpostIdempotencyHandler distribuerJournalpostIdempotencyHandler;
	private final PostadresseService postadresseService;
	private final BestemDistribusjonskanalService bestemDistribusjonskanalService;
	private final JournalpostApi journalpostApi;
	private final PersonnummerService personnummerService;
	private final DokdistadminConsumer dokdistadminConsumer;

	public DistribuerJournalpostService(DistribuerForsendelseProducer distribuerForsendelseProducer,
										DistribuerJournalpostIdempotencyHandler distribuerJournalpostIdempotencyHandler,
										PostadresseService postadresseService,
										BestemDistribusjonskanalService bestemDistribusjonskanalService,
										JournalpostApi journalpostApi,
										PersonnummerService personnummerService,
										DokdistadminConsumer dokdistadminConsumer) {
		this.distribuerForsendelseProducer = distribuerForsendelseProducer;
		this.distribuerJournalpostIdempotencyHandler = distribuerJournalpostIdempotencyHandler;
		this.postadresseService = postadresseService;
		this.bestemDistribusjonskanalService = bestemDistribusjonskanalService;
		this.journalpostApi = journalpostApi;
		this.personnummerService = personnummerService;
		this.dokdistadminConsumer = dokdistadminConsumer;
	}

	public String distribuerForsendelse(DistribuerJournalpost distribuerJournalpost,
										Journalpost journalpost) {

		final String bestillingsId = UUID.randomUUID().toString();

		if (EKSPEDERT.equals(journalpost.getJournalstatus())) {
			return haandterEkspedertJournalpost(distribuerJournalpost);
		}

		validateJournalpostAndDokumenter(journalpost);
		validateTvingKanal(distribuerJournalpost, journalpost);

		Aktoer mottaker = MottakerMapper.map(journalpost.getAvsenderMottaker());

		String personnummer = personnummerService.utledPersonnummer(journalpost.getBruker(), distribuerJournalpost.harPostadresse());

		DistribusjonKanalCode distribusjonKanalCode = bestemDistribusjonskanalService.bestemDistribusjonskanal(distribuerJournalpost, journalpost, personnummer);

		Postadresse postadresse = utledPostadresse(distribuerJournalpost, distribusjonKanalCode, journalpost);

		validatePostadresse(postadresse, mottaker);

		distribuerForsendelse(distribuerJournalpost, postadresse, bestillingsId, journalpost, mottaker, distribusjonKanalCode);

		String persistertBestillingsId = lagreDistribuerJournalpostInfo(distribuerJournalpost.journalpostId(), bestillingsId);

		if (!bestillingsId.equals(persistertBestillingsId)) {
			return persistertBestillingsId;
		}

		oppdaterJournalpostMedTilleggsopplysninger(distribuerJournalpost.journalpostId(), persistertBestillingsId);

		return persistertBestillingsId;
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


	public DistribuerJournalpostInfoResponse hentDistribuerJournalpostInfo(String journalpostId) {
		return distribuerJournalpostIdempotencyHandler.hentDistribuerJournalpostInfo(Long.parseLong(journalpostId));
	}

	private String lagreDistribuerJournalpostInfo(Long journalpostId, String bestillingsId) {
		try {
			distribuerJournalpostIdempotencyHandler.opprettDistribuerJournalpostInfo(journalpostId, bestillingsId);
			return bestillingsId;
		} catch (DataIntegrityViolationException _) {
			log.warn("Samtidig distribusjon av journalpostId={}. En annen request har allerede persistert distribuerJournalpostInfo.", journalpostId);
			DistribuerJournalpostInfoResponse eksisterende = distribuerJournalpostIdempotencyHandler.hentDistribuerJournalpostInfo(journalpostId);

			if (eksisterende == null) {
				throw new JournalpostErAlleredeDistribuertException(
						"Journalpost er allerede distribuert, men fant ikke eksisterende distribuerJournalpostInfo for journalpostId=%s".formatted(journalpostId));
			}

			return eksisterende.bestillingsId();
		}
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

	private String haandterEkspedertJournalpost(DistribuerJournalpost distribuerJournalpost) {
		log.info("Journalpost med journalpostId={} har journalstatus EKSPEDERT. Forsøker å lagre distribuerJournalpostInfo.", distribuerJournalpost.journalpostId());
		FinnForsendelseResponseTo finnForsendelseResponse = dokdistadminConsumer.finnForsendelse(distribuerJournalpost.journalpostId());
		HentForsendelseResponseTo hentForsendelseResponse = dokdistadminConsumer.hentForsendelse(finnForsendelseResponse.forsendelseId());
		return lagreDistribuerJournalpostInfo(distribuerJournalpost.journalpostId(), hentForsendelseResponse.bestillingsId());
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

		return RegoppslagAdresseMapper.map(postadresseService.hentAdresse(avsenderMottaker.getId()));
	}

}
