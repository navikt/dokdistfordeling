package no.nav.dokdistfordeling;

import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import java.util.List;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertHovedokumentFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;

public class Rdist002ValidationUtil {

	private static final String UTGAAENDE = Journalposttype.U.name();

	public void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
		assertNotNullOrEmptyAndCorrectLength("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
		assertNotNullOrEmptyAndCorrectLength("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
	}

	private void assertNotNullOrEmptyAndCorrectLength(String field, String value) {
		assertNotNullOrEmpty(field, value);
		if (value.length() > 20) {
			throw new ValidationException(String.format("%s kan ikke være mer enn 20 tegn", field));
		}
	}

	public void validateAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo, Aktoer mottaker) {
		if (mottaker instanceof Samhandler && adresseTo == null) {
			throw new ValidationException("For mottaker av type samhandler kan ikke adresse være null");
		}

		if (adresseTo != null) {
			assertNotNullOrEmpty("land", adresseTo.getLand());
			assertNotNullOrEmpty("adressetype", adresseTo.getAdressetype());

			if (adresseTo.getAdressetype().equals(NORSK_POSTADRESSE)) {
				assertNotNullOrEmpty("poststed", adresseTo.getPoststed());
				assertNotNullOrEmpty("postnummer", adresseTo.getPostnummer());
			} else if (adresseTo.getAdressetype().equals(UTENLANDSK_POSTADRESSE)) {
				assertNotNullOrEmpty("adresselinje1", adresseTo.getAdresselinje1());
			} else {
				throw new ValidationException(format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType= %s", adresseTo.getAdressetype()));
			}
		}
	}

	public void validateJournalpostAndDokumenter(Journalpost journalpost) {
		assertNotNull(Journalposttype.class, journalpost.getJournalposttype());
		assertParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
		assertParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus(), FERDIGSTILT);

		assertJournalpostFieldNotNull(Journalpost.Bruker.class, journalpost.getBruker());
		assertJournalpostFieldNotNullOrEmpty("brukerId", journalpost.getBruker().getId());
		assertJournalpostFieldNotNull(BrukerIdType.class, journalpost.getBruker().getType());

		assertJournalpostFieldNotNull(Journalpost.AvsenderMottaker.class, journalpost.getAvsenderMottaker());
		assertJournalpostFieldNotNullOrEmpty("mottakerId", journalpost.getAvsenderMottaker().getId());

		validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

		journalpost.getDokumenter().forEach(this::validateDokumentInfo);
	}

	private void validateHovedDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		try {
			assertHovedokumentFieldNotNullOrEmpty("tittel", dokumentInfo.getTittel());
			assertHovedokumentFieldNotNullOrEmpty("brevkode", dokumentInfo.getBrevkode());
		} catch (ValidationException e) {
			throw new ValidationException(format(e.getMessage() + ", dokumentInfoId=%s", dokumentInfo.getDokumentInfoId()));
		}
	}

	private void validateDokumentInfo(Journalpost.DokumentInfo dokumentInfo) {
		if (checkIfNoDokumentvariantWithTilgang(dokumentInfo.getDokumentvarianter())) {
			throw new BrukerManglerTilgangTilDokumentFunctionalException(format("Saksbehandler har ikke tilgang til noen av dokumentets variantformater. dokumentInfoId=%s", dokumentInfo.getDokumentInfoId()));
		}
	}

	private boolean checkIfNoDokumentvariantWithTilgang(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		return dokumentvarianter.stream().noneMatch(dokumentvariant -> dokumentvariant.isSaksbehandlerHarTilgang() && (Variantformat.ARKIV.equals(dokumentvariant.getVariantformat()) || Variantformat.SLADDET.equals(dokumentvariant.getVariantformat())));
	}
}
