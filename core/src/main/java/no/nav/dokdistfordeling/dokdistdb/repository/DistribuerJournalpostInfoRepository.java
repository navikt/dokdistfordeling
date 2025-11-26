package no.nav.dokdistfordeling.dokdistdb.repository;

import no.nav.dokdistfordeling.dokdistdb.domain.DistribuerJournalpostInfo;
import org.springframework.data.repository.CrudRepository;

public interface DistribuerJournalpostInfoRepository extends CrudRepository<DistribuerJournalpostInfo, Long> {
	DistribuerJournalpostInfo findDistribuerJournalpostInfoByJournalpostId(Long journalpostId);
}
