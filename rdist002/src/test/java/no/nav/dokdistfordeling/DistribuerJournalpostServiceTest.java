package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.config.jms.DistribuerForsendelseProducer;
import no.nav.dokdistfordeling.consumer.dokarkiv.JournalpostApi;
import no.nav.dokdistfordeling.consumer.dokdistadmin.DokdistadminConsumer;
import no.nav.dokdistfordeling.consumer.dokdistadmin.FinnForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.dokdistadmin.HentForsendelseResponseTo;
import no.nav.dokdistfordeling.consumer.regoppslag.RegoppslagService;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostIdempotencyHandler;
import no.nav.dokdistfordeling.dokdistdb.DistribuerJournalpostInfoResponse;
import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.exception.functional.JournalpostErAlleredeDistribuertException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static no.nav.dokdistfordeling.TestData.BRUKER_ID;
import static no.nav.dokdistfordeling.TestData.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.TestData.createDistribuerJournalpostBuilder;
import static no.nav.dokdistfordeling.TestData.createJournalpostBuilder;
import static no.nav.dokdistfordeling.constants.ValidationConstants.EKSPEDERT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistribuerJournalpostServiceTest {

	@Mock
	private DistribuerForsendelseProducer distribuerForsendelseProducer;

	@Mock
	private DistribuerJournalpostIdempotencyHandler distribuerJournalpostIdempotencyHandler;

	@Mock
	private BestemDistribusjonskanalService bestemDistribusjonskanalService;

	@Mock
	private JournalpostApi journalpostApi;

	@Mock
	private PersonnummerService personnummerService;

	@Mock
	@SuppressWarnings("unused")
	private RegoppslagService regoppslag;

	@Mock
	private DokdistadminConsumer dokdistadminConsumer;

	@InjectMocks
	private DistribuerJournalpostService distribuerJournalpostService;

	@Test
	void skalReturnereEksisterendeBestillingsIdVedSamtidigDistribusjon() {
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder().build();
		Journalpost journalpost = createJournalpostBuilder().build();
		String eksisterendeBestillingsId = "eksisterende-bestillingsid";

		when(bestemDistribusjonskanalService.bestemDistribusjonskanal(any(), any(), any()))
				.thenReturn(DistribusjonKanalCode.PRINT);
		when(personnummerService.utledPersonnummer(any(), anyBoolean()))
				.thenReturn(BRUKER_ID);

		doThrow(new DataIntegrityViolationException("unique constraint violated"))
				.when(distribuerJournalpostIdempotencyHandler).opprettDistribuerJournalpostInfo(eq(JOURNALPOST_ID), any());

		when(distribuerJournalpostIdempotencyHandler.hentDistribuerJournalpostInfo(JOURNALPOST_ID))
				.thenReturn(new DistribuerJournalpostInfoResponse(JOURNALPOST_ID, eksisterendeBestillingsId));

		String resultat = distribuerJournalpostService.distribuerForsendelse(distribuerJournalpost, journalpost);

		assertThat(resultat).isEqualTo(eksisterendeBestillingsId);
		verify(journalpostApi, never()).oppdaterJournalpost(anyLong(), any());
	}

	@Test
	void skalKasteExceptionDersomEksisterendeDistribusjonIkkeFinnesEtterFeiletPersistering() {
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder().build();
		Journalpost journalpost = createJournalpostBuilder().build();

		when(bestemDistribusjonskanalService.bestemDistribusjonskanal(any(), any(), any()))
				.thenReturn(DistribusjonKanalCode.PRINT);
		when(personnummerService.utledPersonnummer(any(), anyBoolean()))
				.thenReturn(BRUKER_ID);

		doThrow(new DataIntegrityViolationException("unique constraint violated"))
				.when(distribuerJournalpostIdempotencyHandler).opprettDistribuerJournalpostInfo(eq(JOURNALPOST_ID), any());

		when(distribuerJournalpostIdempotencyHandler.hentDistribuerJournalpostInfo(JOURNALPOST_ID))
				.thenReturn(null);

		assertThatExceptionOfType(JournalpostErAlleredeDistribuertException.class)
				.isThrownBy(() -> distribuerJournalpostService.distribuerForsendelse(distribuerJournalpost, journalpost))
				.withMessage("Journalpost er allerede distribuert, men fant ikke eksisterende distribuerJournalpostInfo for journalpostId=%s", JOURNALPOST_ID);

		verify(journalpostApi, never()).oppdaterJournalpost(anyLong(), any());
	}

	@Test
	void skalHenteBestillingsIdFraDokdistadminOgPersistereNaarJournalpostErEkspedert() {
		String eksisterendeBestillingsId = "bestillingsid-fra-dokdistadmin";
		long forsendelseId = 99999L;
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder().build();
		Journalpost journalpost = createJournalpostBuilder()
				.journalstatus(EKSPEDERT)
				.build();

		when(dokdistadminConsumer.finnForsendelse(JOURNALPOST_ID))
				.thenReturn(new FinnForsendelseResponseTo(forsendelseId));
		when(dokdistadminConsumer.hentForsendelse(forsendelseId))
				.thenReturn(new HentForsendelseResponseTo(forsendelseId, eksisterendeBestillingsId));

		String resultat = distribuerJournalpostService.distribuerForsendelse(distribuerJournalpost, journalpost);

		assertThat(resultat).isEqualTo(eksisterendeBestillingsId);
		verify(distribuerJournalpostIdempotencyHandler).opprettDistribuerJournalpostInfo(JOURNALPOST_ID, eksisterendeBestillingsId);
		verify(journalpostApi, never()).oppdaterJournalpost(anyLong(), any());
		verify(distribuerForsendelseProducer, never()).produce(any(), any(), any());
	}
}
