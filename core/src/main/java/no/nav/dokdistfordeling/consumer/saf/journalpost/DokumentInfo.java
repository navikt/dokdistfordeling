package no.nav.dokdistfordeling.consumer.saf.journalpost;

import lombok.Builder;
import lombok.Value;
import no.nav.dokdistfordeling.kodeverk.Dokumentstatus;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class DokumentInfo {
	private final String dokumentInfoId;
	private final String tittel;
	private final String brevkode;
	private final Dokumentstatus dokumentstatus;

	@Builder.Default
	private final List<Dokumentvariant> dokumentvarianter = new ArrayList<>();
}
