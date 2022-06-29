package no.nav.dokdistfordeling.consumer.saf.graphql;

import no.nav.dokdistfordeling.consumer.saf.journalpost.SafJournalpostTo;

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

		assertJournalpostFieldNotNull(SafJournalpostTo.Bruker.class, safJournalpostTo.getBruker());
		validateBruker(safJournalpostTo.getBruker());

		/*
		 * Tom mottakerId er OK i rdist002
		 * Qdist012 henter også ut journalpost fra saf men det er kun for å hente vedlegg - dette kan vel da bare fjernes?
		assertJournalpostFieldNotNull(SafJournalpostTo.AvsenderMottaker.class, safJournalpostTo.getAvsenderMottaker());
		validateAvsenderMottaker(safJournalpostTo.getAvsenderMottaker());
		*/

		assertJournalpostFieldNotNull(SafJournalpostTo.DokumentInfo.class, safJournalpostTo.getDokumenter());
		validateDokumenter(safJournalpostTo.getDokumenter());

		return safJournalpostTo;
	}

	private void validateAvsenderMottaker(SafJournalpostTo.AvsenderMottaker avsenderMottaker) {
		assertNotNullOrEmpty("mottakerId", avsenderMottaker.getId());
	}

	private void validateDokumenter(List<SafJournalpostTo.DokumentInfo> dokumenter) {
		dokumenter.forEach(this::validateDokument);
	}

	private void validateDokument(SafJournalpostTo.DokumentInfo dokumentInfo) {
		assertDokumentFieldNotNullOrEmpty("dokumentInfoId", dokumentInfo.getDokumentInfoId());
		validateDokumentVarianter(dokumentInfo.getDokumentvarianter());
	}

	private void validateDokumentVarianter(List<SafJournalpostTo.Dokumentvariant> dokumentvarianter) {
		dokumentvarianter.forEach(this::validateAndReturnDokumentVariant);
	}

	private void validateAndReturnDokumentVariant(SafJournalpostTo.Dokumentvariant dokumentvariant) {
		assertNotNullOrEmpty("variantformat", dokumentvariant.getVariantformat());
	}

	private void validateBruker(SafJournalpostTo.Bruker bruker) {
		assertNotNullOrEmpty("brukerId", bruker.getId());
		assertNotNullOrEmpty("brukerIdType", bruker.getType());
	}

}
