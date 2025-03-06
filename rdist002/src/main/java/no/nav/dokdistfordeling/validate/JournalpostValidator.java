package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.InvalidFiltypeException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertHovedokumentFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostParameterIsAsExpected;

public class JournalpostValidator {

	public static final String PDF = "PDF";
	public static final String PDFA = "PDFA";

	private static final String UTGAAENDE = Journalposttype.U.name();
	private static final EnumSet<Variantformat> VARIANTFORMATS = EnumSet.of(ARKIV, SLADDET);
	private static final Set<String> FILETYPE_PDF_PDFA = Set.of(PDF, PDFA);

	private JournalpostValidator() {
	}

	public static void validateJournalpostAndDokumenter(Journalpost journalpost) {
		assertJournalpostFieldNotNull(Journalposttype.class, journalpost.getJournalposttype());
		assertJournalpostParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
		assertJournalpostParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus(), FERDIGSTILT);

		assertJournalpostFieldNotNull(Journalpost.Bruker.class, journalpost.getBruker());
		assertJournalpostFieldNotNull(BrukerIdType.class, journalpost.getBruker().getType());
		assertJournalpostFieldNotNull(Journalpost.AvsenderMottaker.class, journalpost.getAvsenderMottaker());

		assertJournalpostFieldNotNullOrEmpty("brukerId", journalpost.getBruker().getId());
		assertJournalpostFieldNotNullOrEmpty("mottakerNavn", journalpost.getAvsenderMottaker().getNavn());

		validateHovedDokumentInfo(journalpost.getDokumenter().getFirst());

		journalpost.getDokumenter().forEach(JournalpostValidator::validateDokumentInfo);
	}

	private static void validateHovedDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		try {
			assertHovedokumentFieldNotNullOrEmpty("tittel", dokumentInfo.getTittel());
			assertHovedokumentFieldNotNullOrEmpty("brevkode", dokumentInfo.getBrevkode());
		} catch (ValidationException e) {
			throw new ValidationException(format(e.getMessage() + ", dokumentInfoId=%s", dokumentInfo.getDokumentInfoId()));
		}
	}

	private static void validateDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		if (checkIfNoDokumentvariantWithTilgang(dokumentInfo.getDokumentvarianter())) {
			throw new BrukerManglerTilgangTilDokumentFunctionalException(
					format("Systembruker eller saksbehandler har ikke tilgang til dokumentInfoId=%s og kan derfor ikke bestille distribusjon. " +
						   "For saksbehandlere betyr dette ofte at saksbehandleren mangler tilgang til tema eller brukers enhet i AXSYS. " +
						   "For systembrukere betyr dette ofte at systembrukeren ikke ligger inn med riktig tema-role i Azure IAC-konfigurasjonen for SAF sin <env-config.json>. " +
						   "Kontakt oss på #team_dokumentløsninger for bistand.", dokumentInfo.getDokumentInfoId())
			);
		}

		checkIfNoDokumentvariantWithFiltypePdfOrPdfA(dokumentInfo.getDokumentvarianter());
	}


	private static boolean checkIfNoDokumentvariantWithTilgang(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		return dokumentvarianter.stream()
				.noneMatch(dokumentvariant -> dokumentvariant.isSaksbehandlerHarTilgang() && VARIANTFORMATS.contains(dokumentvariant.getVariantformat()));
	}

	private static void checkIfNoDokumentvariantWithFiltypePdfOrPdfA(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		dokumentvarianter.forEach(dokumentvariant -> {
			if (!(VARIANTFORMATS.contains(dokumentvariant.getVariantformat()) && FILETYPE_PDF_PDFA.contains(dokumentvariant.getFiltype()))) {
				throw new InvalidFiltypeException(format("Ugyldig dokumentvariant=%s eller filtype=%s, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres", dokumentvariant.getVariantformat(), dokumentvariant.getFiltype()));
			}
		});
	}
}
