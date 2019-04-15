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

		// todo merge with consumer?
		return safGraphqlConsumer.performQuery(GraphQLRequest.builder()
						.query(journalpostquery)
						.operationName("journalpost")
						.variables(Collections.singletonMap("queryJournalpostId", journalpostid))
						.build(),
				authorizationHeader);

//		return Journalpost.builder() // todo saved for test
//				.journalposttype(Journalposttype.U)
//				.journalstatus(Journalstatus.FERDIGSTILT)
//				.bruker(new Bruker("54321", BrukerIdType.FNR))
//				.avsenderMottaker(AvsenderMottaker.builder().erLikBruker(true).build())
//				.dokumenter(Arrays.asList(
//						DokumentInfo.builder()
//								.tittel("The red wedding")
//								.brevkode("something")
//								.dokumentstatus(Dokumentstatus.FERDIGSTILT)
//								.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
//										.filnavn("Something else")
//										.saksbehandlerHarTilgang(true)
//										.variantformat(Variantformat.ARKIV).build()))
//								.build(),
//						DokumentInfo.builder()
//								.tittel("The blue wedding")
//								.brevkode("something other than else")
//								.dokumentstatus(Dokumentstatus.FERDIGSTILT)
//								.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
//										.filnavn("Something elseif")
//										.saksbehandlerHarTilgang(true)
//										.variantformat(Variantformat.ARKIV).build()))
//								.build()))
//				.build();
	}
}