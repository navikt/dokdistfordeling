package no.nav.dokdistfordeling.itest.config;

import no.nav.dokdistfordeling.dokdistdb.config.DokdistadminRepositoryConfig;
import no.nav.dokdistfordeling.dokdistdb.repository.DistribuerJournalpostInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static no.nav.dokdistfordeling.constants.Constants.USER_ID;

@DataJpaTest
@ContextConfiguration(classes = {DokdistadminRepositoryConfig.class})
@ActiveProfiles("itest")
public class AbstractRepositoryTest {

	@Autowired
	protected DistribuerJournalpostInfoRepository distribuerJournalpostInfoRepository;

	@BeforeEach
	void setUp() {
		if (MDC.get(USER_ID) == null) {
			MDC.put(USER_ID, "repoTest");
		}
		emptyDatabases();
	}

	void emptyDatabases() {
		distribuerJournalpostInfoRepository.deleteAll();
	}
}
