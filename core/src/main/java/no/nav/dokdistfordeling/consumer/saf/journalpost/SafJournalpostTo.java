package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class SafJournalpostTo {
	private final String tittel;
	private final String tema;
	private final String journalposttype;
	private final String journalstatus;
	private final Bruker bruker;
	private final AvsenderMottaker avsenderMottaker;

	@Builder.Default
	private final List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class Bruker {
		private final String id;
		private final String type;
	}

	@Value
	@Builder
	public static class AvsenderMottaker {
		private final String id;
		private final String navn;
		private final String type;
	}

	@Value
	@Builder
	public static class DokumentInfo {
		private final String dokumentInfoId;
		private final String tittel;
		private final String brevkode;
		private final String dokumentstatus;

		@Builder.Default
		private final List<Dokumentvariant> dokumentvarianter = new ArrayList<>();
	}

	@Value
	@Builder
	public static class Dokumentvariant {
		private final String variantformat;
		private final boolean saksbehandlerHarTilgang;
	}
}
