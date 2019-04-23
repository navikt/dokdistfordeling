package no.nav.dokdistfordeling.consumer.saf;


import no.nav.dokdistfordeling.consumer.saf.graphql.GraphQLRequest;
import no.nav.dokdistfordeling.consumer.saf.graphql.SafGraphqlConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class SafJournalpostQueryServiceImpl implements SafJournalpostQueryService {

	private final SafGraphqlConsumer safGraphqlConsumer;
	private final String journalpostquery =
			"query journalpost($queryJournalpostId: String!) {\n" +
					"  journalpost(journalpostId: $queryJournalpostId) {\n" +
					"    journalposttype\n" +
					"    journalstatus\n" +
					"    tema\n" +
					"    bruker {\n" +
					"      id\n" +
					"      type\n" +
					"    }\n" +
					"    avsenderMottaker {\n" +
					"      id\n" +
					"      navn\n" +
					"      land\n" +
					"      erLikBruker\n" +
					"    }\n" +
					"    dokumenter {\n" +
					"      dokumentInfoId\n" +
					"      tittel\n" +
					"      brevkode\n" +
					"      dokumentstatus\n" +
					"      dokumentvarianter {\n" +
					"        saksbehandlerHarTilgang\n" +
					"        variantformat\n" +
					"      }\n" +
					"    }\n" +
					"  }\n" +
					"}\n";

	public SafJournalpostQueryServiceImpl(SafGraphqlConsumer safGraphqlConsumer) {
		this.safGraphqlConsumer = safGraphqlConsumer;
	}

	public Journalpost hentJournalpost(String journalpostid, String authorizationHeader) {

		return safGraphqlConsumer.performQuery(GraphQLRequest.builder()
						.query(journalpostquery)
						.operationName("journalpost")
						.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
						.build(),
				authorizationHeader);
	}
}