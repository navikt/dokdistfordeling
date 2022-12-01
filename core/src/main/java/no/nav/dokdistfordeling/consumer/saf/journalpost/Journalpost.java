package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class Journalpost {
	String tittel;
	String tema;
	Journalposttype journalposttype;
	String journalstatus;
	Tilleggsopplysninger tilleggsopplysninger;
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
		BrukerIdType type;
	}

	@Value
	@Builder
	public static class AvsenderMottaker {
		String id;
		String navn;
		AvsenderMottakerIdType type;
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
		Variantformat variantformat;
		boolean saksbehandlerHarTilgang;
	}

}
