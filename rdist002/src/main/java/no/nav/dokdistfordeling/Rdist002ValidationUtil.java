package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertHovedokumentFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertJournalpostFieldNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNull;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullAndValidValueIgnoreCase;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertParameterIsAsExpected;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertStringIsNumberOfExactLength;

@Slf4j
public class Rdist002ValidationUtil {

	private static final String UTGAAENDE = Journalposttype.U.name();
	private final Set<String> ISO3166_TWO_LETTER_CODES;

	public Rdist002ValidationUtil() {
		ISO3166_TWO_LETTER_CODES = Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());
	}

	public void validateRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
		assertNotNullOrEmptyAndCorrectLength("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
		assertNotNullOrEmptyAndCorrectLength("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
		assertNotNullAndValidValueIgnoreCase("distribusjonstype", distribuerJournalpostRequestTo.getDistribusjonstype(), DistribusjonstypeCode.values());
		assertNotNullAndValidValueIgnoreCase("distribusjonstidspunkt", distribuerJournalpostRequestTo.getDistribusjonstidspunkt(), DistribusjonstidspunktCode.values());
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
			validateLandKode(adresseTo.getLand());

			if (NORSK_POSTADRESSE.equals(adresseTo.getAdressetype())) {
				assertNotNullOrEmpty("poststed", adresseTo.getPoststed());
				assertStringIsNumberOfExactLength("postnummer", adresseTo.getPostnummer(), 4);

			} else if (UTENLANDSK_POSTADRESSE.equals(adresseTo.getAdressetype())) {
				assertNotNullOrEmpty("adresselinje1", adresseTo.getAdresselinje1());
			} else {
				throw new ValidationException(format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType= %s", adresseTo.getAdressetype()));
			}
		}
	}

	private void validateLandKode(String land) {
		if (!ISO3166_TWO_LETTER_CODES.contains(land)) {
			throw new ValidationException(format("Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=%s", land));
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
		assertJournalpostFieldNotNullOrEmpty("mottakerNavn", journalpost.getAvsenderMottaker().getNavn());

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
