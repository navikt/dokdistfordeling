package no.nav.dokdistfordeling.dokdistdb;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.dokdistdb.domain.DistribuerJournalpostInfo;
import no.nav.dokdistfordeling.dokdistdb.repository.DistribuerJournalpostInfoRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static no.nav.dokdistfordeling.constants.Constants.USER_ID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DistribuerJournalpostInfoService {

	private final DistribuerJournalpostInfoRepository distribuerJournalpostInfoRepository;

	public DistribuerJournalpostInfoService(DistribuerJournalpostInfoRepository distribuerJournalpostInfoRepository) {
		this.distribuerJournalpostInfoRepository = distribuerJournalpostInfoRepository;
	}

	@Transactional
	public void opprettDistribuerJournalpostInfo(Long journalpostId, String bestillingsId) {

		log.info("opprettDistribuerJournalpostInfo har mottatt kall om å persistere distribuerJournalpostInfo for journalpostId={}", journalpostId);
		DistribuerJournalpostInfo distribuerJournalpostInfo = DistribuerJournalpostInfo.builder()
				.arkivkode(journalpostId)
				.dokumentId(bestillingsId)
				.opprettetAv(getUserId())
				.opprettetDato(LocalDateTime.now())
				.build();

		distribuerJournalpostInfoRepository.save(distribuerJournalpostInfo);
		log.info("Laget distribuerJournalpostInfo for journalpostId={}", journalpostId);
	}

	public DistribuerJournalpostInfoResponse hentDistribuerJournalpostInfo(Long journalpostId) {
		DistribuerJournalpostInfo distribuerJournalpost = distribuerJournalpostInfoRepository.findDistribuerJournalpostInfoByArkivkode(journalpostId);
		return distribuerJournalpost == null ? null : DistribuerJournalpostInfoResponse.builder()
				.journalpostId(distribuerJournalpost.getArkivkode())
				.bestillingsId(distribuerJournalpost.getDokumentId())
				.build();
	}

	private static String getUserId() {
		return MDC.get(USER_ID);
	}
}
