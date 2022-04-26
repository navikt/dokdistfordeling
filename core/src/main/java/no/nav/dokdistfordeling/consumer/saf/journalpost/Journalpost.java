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
	private final String tittel;
	private final String tema;
	private final Journalposttype journalposttype;
	private final String journalstatus;
	private final Tilleggsopplysninger tilleggsopplysninger;
	private final Bruker bruker;
	private final AvsenderMottaker avsenderMottaker;

	@Builder.Default
	private final List<DokumentInfo> dokumenter = new ArrayList<>();

	@Value
	@Builder
	public static class Tilleggsopplysninger {
		private final String nokkel;
		private final String verdi;
	}

	@Value
	@Builder
	public static class Bruker {
		private final String id;
		private final BrukerIdType type;
	}

	@Value
	@Builder
	public static class AvsenderMottaker {
		private final String id;
		private final String navn;
		private final AvsenderMottakerIdType type;
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
		private final Variantformat variantformat;
		private final boolean saksbehandlerHarTilgang;
	}

}
