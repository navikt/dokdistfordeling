package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@Value
@Builder
public class Journalpost {
	private final String tittel;
	private final String tema;
	private final Journalposttype journalposttype;
	private final Journalstatus journalstatus;
	private final Bruker bruker;
	private final AvsenderMottaker avsenderMottaker;

	@Builder.Default
	private final List<DokumentInfo> dokumenter = new ArrayList<>();
}
