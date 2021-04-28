package no.nav.dokdistfordeling.consumer.saf;


import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.graphql.GraphQLRequest;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToMapper;
import no.nav.dokdistfordeling.consumer.saf.graphql.JournalpostToValidator;
import no.nav.dokdistfordeling.consumer.saf.graphql.SafGraphqlConsumer;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class SafJournalpostQueryServiceImpl implements SafJournalpostQueryService {

	private static final String JOURNALPOST_QUERY =
			"query journalpost($queryJournalpostId: String!) {\n" +
					"  journalpost(journalpostId: $queryJournalpostId) {\n" +
					"    tittel\n" +
					"    journalposttype\n" +
					"    journalstatus\n" +
					"    tema\n" +
					"    bruker {\n" +
					"      id\n" +
					"      type\n" +
					"    }\n" +
					"    avsenderMottaker {\n" +
					"      id\n" +
					"      type\n" +
					"      navn\n" +
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
	private final SafGraphqlConsumer safGraphqlConsumer;
	private final JournalpostToMapper journalpostMapper = new JournalpostToMapper();
	private final JournalpostToValidator journalpostToValidator = new JournalpostToValidator();

	public SafJournalpostQueryServiceImpl(SafGraphqlConsumer safGraphqlConsumer) {
		this.safGraphqlConsumer = safGraphqlConsumer;
	}

	public Journalpost hentJournalpost(String journalpostid, String authorizationHeader) {


		SafJournalpostTo safJournalpostTo = safGraphqlConsumer.performQuery(GraphQLRequest.builder()
				.query(JOURNALPOST_QUERY)
				.operationName("journalpost")
				.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
				.build(), authorizationHeader);

		log.info("hentJournalpost - tittel {}", safJournalpostTo.getTittel());

		return journalpostMapper.map(journalpostToValidator.validateAndReturn(safJournalpostTo));
//
//
//		return journalpostMapper.map(
//				journalpostToValidator.validateAndReturn(
//						safGraphqlConsumer.performQuery(GraphQLRequest.builder()
//								.query(JOURNALPOST_QUERY)
//								.operationName("journalpost")
//								.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
//								.build(), authorizationHeader))
//		);
	}
}