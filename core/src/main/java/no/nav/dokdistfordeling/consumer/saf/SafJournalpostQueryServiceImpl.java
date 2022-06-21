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
			"query journalpost($queryJournalpostId: String!) {\n" +
					"  journalpost(journalpostId: $queryJournalpostId) {\n" +
					"    tittel\n" +
					"    journalposttype\n" +
					"    journalstatus\n" +
					"    tema\n" +
					"    tilleggsopplysninger {\n" +
					"      nokkel\n" +
					"      verdi\n" +
					"    }\n" +
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

	private static final String JOURNALPOSTSTATUS_QUERY =
			"query {\n" +
					"	journalpost(journalpostId: $queryJournalpostId) {\n" +
					"		journalstatus\n" +
					"	}\n" +
					"};\n";

}