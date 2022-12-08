package no.nav.dokdistfordeling.consumer.saf.graphql;

import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo.Dokumentvariant;

import java.util.List;

import static no.nav.dokdistfordeling.util.ValidationUtil.assertDokumentFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;

public class JournalpostToValidator {
	public SafJournalpostTo validateAndReturn(SafJournalpostTo safJournalpostTo) {
		assertJournalpostFieldNotNullOrEmpty("journalposttype", safJournalpostTo.getJournalposttype());
		assertJournalpostFieldNotNullOrEmpty("journalstatus", safJournalpostTo.getJournalstatus());
		assertJournalpostFieldNotNullOrEmpty("tema", safJournalpostTo.getTema());

		assertJournalpostFieldNotNull(Bruker.class, safJournalpostTo.getBruker());
		validateBruker(safJournalpostTo.getBruker());


		assertJournalpostFieldNotNull(DokumentInfo.class, safJournalpostTo.getDokumenter());
		validateDokumenter(safJournalpostTo.getDokumenter());

		return safJournalpostTo;
	}

	private void validateDokumenter(List<DokumentInfo> dokumenter) {
		dokumenter.forEach(this::validateDokument);
	}

	private void validateDokument(DokumentInfo dokumentInfo) {
		assertDokumentFieldNotNullOrEmpty("dokumentInfoId", dokumentInfo.getDokumentInfoId());
		validateDokumentVarianter(dokumentInfo.getDokumentvarianter());
	}

	private void validateDokumentVarianter(List<Dokumentvariant> dokumentvarianter) {
		dokumentvarianter.forEach(this::validateAndReturnDokumentVariant);
	}

	private void validateAndReturnDokumentVariant(Dokumentvariant dokumentvariant) {
		assertNotNullOrEmpty("variantformat", dokumentvariant.getVariantformat());
	}

	private void validateBruker(Bruker bruker) {
		assertNotNullOrEmpty("brukerId", bruker.getId());
		assertNotNullOrEmpty("brukerIdType", bruker.getType());
	}

}
