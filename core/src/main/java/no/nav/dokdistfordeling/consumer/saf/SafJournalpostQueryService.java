package no.nav.dokdistfordeling.consumer.saf;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;

public interface SafJournalpostQueryService {

	Journalpost hentJournalpost(String journalpostid, String authorizationHeader);

}
