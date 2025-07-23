package no.nav.dokdistfordeling.consumer.saf.journalpost;

import no.nav.dokdistfordeling.consumer.saf.graphql.GraphQLRequest;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToMapper;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToValidator;
import no.nav.dokdistfordeling.consumer.saf.graphql.SafGraphqlConsumer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class SafJournalpostQueryService {

	private final SafGraphqlConsumer safGraphqlConsumer;
	private final JournalpostToMapper journalpostMapper = new JournalpostToMapper();
	private final JournalpostToValidator journalpostToValidator = new JournalpostToValidator();

	public SafJournalpostQueryService(SafGraphqlConsumer safGraphqlConsumer) {
		this.safGraphqlConsumer = safGraphqlConsumer;
	}

	public Journalpost hentJournalpost(String journalpostid) {
		return hentJournalpost(journalpostid, Optional.empty());
	}

	public Journalpost hentJournalpost(String journalpostid, Optional<String> authorizationHeader) {
		return journalpostMapper.map(
				journalpostToValidator.validateAndReturn(
						safGraphqlConsumer.performQuery(GraphQLRequest.builder()
								.query(JOURNALPOST_QUERY)
								.operationName("journalpost")
								.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
								.build(), authorizationHeader))
		);
	}

	public String hentJournalpostStatus(String journalpostid) {
		return safGraphqlConsumer.performQuery(GraphQLRequest.builder()
				.query(JOURNALPOSTSTATUS_QUERY)
				.operationName("journalpost")
				.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
				.build(), Optional.empty()).getJournalstatus();
	}

	private static final String JOURNALPOST_QUERY =
			"""
					query journalpost($queryJournalpostId: String!) {
					  journalpost(journalpostId: $queryJournalpostId) {
					    tittel
					    journalposttype
					    journalstatus
					    tema
					    tilleggsopplysninger {
					      nokkel
					      verdi
					    }
					    bruker {
					      id
					      type
					    }
					    avsenderMottaker {
					      id
					      type
					      navn
					    }
					    dokumenter {
					      dokumentInfoId
					      tittel
					      brevkode
					      dokumentstatus
					      dokumentvarianter {
					        saksbehandlerHarTilgang
					        variantformat
					        filtype
					        filstoerrelse
					      }
					    }
					  }
					}
					""";

	private static final String JOURNALPOSTSTATUS_QUERY =
			"""
					query journalpost($queryJournalpostId: String!) {
					  journalpost(journalpostId: $queryJournalpostId) { journalstatus
						}
					}
					""";

}
