package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class SafJournalpostTo {
	String tittel;
	String tema;
	String journalposttype;
	String journalstatus;
	List<Tilleggsopplysninger> tilleggsopplysninger;
	Bruker bruker;
	AvsenderMottaker avsenderMottaker;

	@Builder.Default
	List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class Tilleggsopplysninger {
		String nokkel;
		String verdi;
	}

	@Value
	@Builder
	public static class Bruker {
		String id;
		String type;
	}

	@Value
	@Builder
	public static class AvsenderMottaker {
		String id;
		String navn;
		String type;
	}

	@Value
	@Builder
	public static class DokumentInfo {
		String dokumentInfoId;
		String tittel;
		String brevkode;
		String dokumentstatus;

		@Builder.Default
		List<Dokumentvariant> dokumentvarianter = new ArrayList<>();
	}

	@Value
	@Builder
	public static class Dokumentvariant {
		String variantformat;
		String filtype;
		boolean saksbehandlerHarTilgang;
		int filstoerrelse;
	}
}
