package no.nav.dokdistfordeling.consumer.saf.graphql;

import static no.nav.dokdistfordeling.util.MappingUtil.stringToEnum;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;
import no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;

import java.util.List;
import java.util.stream.Collectors;

public class JournalpostToMapper {

	public Journalpost map(SafJournalpostTo safJournalpostTo) {
		return Journalpost.builder()
				.tittel(safJournalpostTo.getTittel())
				.journalposttype(stringToEnum(Journalposttype.class, safJournalpostTo.getJournalposttype()))
				.journalstatus(safJournalpostTo.getJournalstatus())
				.tema(safJournalpostTo.getTema())
				.bruker(mapBruker(safJournalpostTo.getBruker()))
				.avsenderMottaker(mapAvsenderMottaker(safJournalpostTo.getAvsenderMottaker()))
				.dokumenter(mapDokumenter(safJournalpostTo.getDokumenter()))
				.build();
	}

	private List<Journalpost.DokumentInfo> mapDokumenter(List<SafJournalpostTo.DokumentInfo> dokumenter) {
		return dokumenter
				.stream()
				.map(this::mapDokument)
				.collect(Collectors.toList());
	}

	private Journalpost.DokumentInfo mapDokument(SafJournalpostTo.DokumentInfo dokumentInfo) {
		return Journalpost.DokumentInfo.builder()
				.dokumentInfoId(dokumentInfo.getDokumentInfoId())
				.tittel(dokumentInfo.getTittel())
				.brevkode(dokumentInfo.getBrevkode())
				.dokumentstatus(dokumentInfo.getDokumentstatus())
				.dokumentvarianter(mapDokumentVarianter(dokumentInfo.getDokumentvarianter()))
				.build();
	}

	private List<Journalpost.Dokumentvariant> mapDokumentVarianter(List<SafJournalpostTo.Dokumentvariant> dokumentvarianter) {
		return dokumentvarianter
				.stream()
				.filter(this::isVariantformatArkivOrSladdet)
				.map(this::mapDokumentVariant)
				.collect(Collectors.toList());

	}

	private Journalpost.Dokumentvariant mapDokumentVariant(SafJournalpostTo.Dokumentvariant dokumentvariant) {
		return Journalpost.Dokumentvariant.builder()
				.variantformat(stringToEnum(Variantformat.class, dokumentvariant.getVariantformat()))
				.saksbehandlerHarTilgang(dokumentvariant.isSaksbehandlerHarTilgang())
				.build();
	}

	private Journalpost.Bruker mapBruker(SafJournalpostTo.Bruker bruker) {
		return Journalpost.Bruker.builder()
				.id(bruker.getId())
				.type(stringToEnum(BrukerIdType.class, bruker.getType()))
				.build();
	}

	private Journalpost.AvsenderMottaker mapAvsenderMottaker(SafJournalpostTo.AvsenderMottaker avsenderMottaker) {
		return Journalpost.AvsenderMottaker.builder()
				.id(avsenderMottaker.getId())
				.navn(avsenderMottaker.getNavn())
				.type(stringToEnum(AvsenderMottakerIdType.class, avsenderMottaker.getType()))
				.build();
	}

	private boolean isVariantformatArkivOrSladdet(SafJournalpostTo.Dokumentvariant dokumentvariant) {
		return Variantformat.ARKIV.name().equals(dokumentvariant.getVariantformat()) || Variantformat.SLADDET.name().equals(dokumentvariant.getVariantformat());
	}
}
