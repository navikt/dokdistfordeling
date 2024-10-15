package no.nav.dokdistfordeling.consumer.saf;


import no.nav.dokdistfordeling.consumer.saf.graphql.GraphQLRequest;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToMapper;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToValidator;
import no.nav.dokdistfordeling.consumer.saf.graphql.SafGraphqlConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.sts.StsRestConsumer;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static no.nav.dokdistfordeling.constants.Constants.BEARER_PREFIX;

@Component
public class SafJournalpostQueryServiceImpl implements SafJournalpostQueryService {

	private final SafGraphqlConsumer safGraphqlConsumer;
	private final StsRestConsumer stsRestConsumer;
	private final JournalpostToMapper journalpostMapper = new JournalpostToMapper();
	private final JournalpostToValidator journalpostToValidator = new JournalpostToValidator();

	public SafJournalpostQueryServiceImpl(SafGraphqlConsumer safGraphqlConsumer, StsRestConsumer stsRestConsumer) {
		this.safGraphqlConsumer = safGraphqlConsumer;
		this.stsRestConsumer = stsRestConsumer;
	}

	@Override
	public Journalpost hentJournalpost(String journalpostid) {
		return hentJournalpost(journalpostid, getAuthorizationHeader());
	}

	@Override
	public Journalpost hentJournalpost(String journalpostid, String authorizationHeader) {
		return journalpostMapper.map(
				journalpostToValidator.validateAndReturn(
						safGraphqlConsumer.performQuery(GraphQLRequest.builder()
								.query(JOURNALPOST_QUERY)
								.operationName("journalpost")
								.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
								.build(), authorizationHeader))
		);
	}

	@Override
	public String hentJournalpostStatus(String journalpostid) {
		return safGraphqlConsumer.performQuery(GraphQLRequest.builder()
				.query(JOURNALPOSTSTATUS_QUERY)
				.operationName("journalpost")
				.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
				.build(), getAuthorizationHeader()).getJournalstatus();
	}

	private String getAuthorizationHeader() {
		return BEARER_PREFIX + stsRestConsumer.getOidcToken();
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