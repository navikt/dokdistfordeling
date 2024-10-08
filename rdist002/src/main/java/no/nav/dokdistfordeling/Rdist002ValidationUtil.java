package no.nav.dokdistfordeling;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.exception.functional.BrukerManglerTilgangTilDokumentFunctionalException;
import no.nav.dokdistfordeling.exception.functional.InvalidFiltypeException;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Variantformat;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.constants.ValidationConstants.FERDIGSTILT;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
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
	private static final Set<String> ISO3166_TWO_LETTER_CODES = Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());
	private static final String KOSOVO_LAND_KODE = "XK";
	public static final String PDF = "PDF";
	public static final String PDFA = "PDFA";

	private static final EnumSet<Variantformat> VARIANTFORMATS = EnumSet.of(ARKIV, SLADDET);
	private static final Set<String> FILETYPE_PDF_PDFA = Set.of(PDF, PDFA);


	static {
		ISO3166_TWO_LETTER_CODES.add(KOSOVO_LAND_KODE);
	}

	public static void validateDistribuerJournalpostRequest(DistribuerJournalpostRequestTo distribuerJournalpostRequestTo) {
		assertNotNullOrEmpty("journalpostId", distribuerJournalpostRequestTo.getJournalpostId());
		assertNotNullOrEmptyAndCorrectLength("bestillendeFagsystem", distribuerJournalpostRequestTo.getBestillendeFagsystem());
		assertNotNullOrEmptyAndCorrectLength("dokumentProdapp", distribuerJournalpostRequestTo.getDokumentProdApp());
		assertNotNullAndValidValueIgnoreCase("distribusjonstype", distribuerJournalpostRequestTo.getDistribusjonstype(), DistribusjonstypeCode.values());
		assertNotNullAndValidValueIgnoreCase("distribusjonstidspunkt", distribuerJournalpostRequestTo.getDistribusjonstidspunkt(), DistribusjonstidspunktCode.values());
	}

	private static void assertNotNullOrEmptyAndCorrectLength(String field, String value) {
		assertNotNullOrEmpty(field, value);
		if (value.length() > 20) {
			throw new ValidationException(format("%s kan ikke være mer enn 20 tegn", field));
		}
	}

	public static void validateAdresse(DistribuerJournalpostRequestTo.AdresseTo adresseTo, Aktoer mottaker) {
		if (mottaker instanceof Samhandler && adresseTo == null) {
			throw new ValidationException("For mottaker av type samhandler kan ikke adresse være null");
		}

		if (adresseTo != null) {
			validateLandKode(adresseTo.getLand());

			if (NORSK_POSTADRESSE.equals(adresseTo.getAdressetype())) {
				assertNotNullOrEmpty("poststed", adresseTo.getPoststed());
				assertNotNullOrEmpty("postnummer", adresseTo.getPostnummer());
				assertStringIsNumberOfExactLength("postnummer", adresseTo.getPostnummer().strip(), 4);

			} else if (UTENLANDSK_POSTADRESSE.equals(adresseTo.getAdressetype())) {
				assertNotNullOrEmpty("adresselinje1", adresseTo.getAdresselinje1());
			} else {
				throw new ValidationException(format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType= %s", adresseTo.getAdressetype()));
			}
		}
	}

	private static void validateLandKode(String land) {
		if (!ISO3166_TWO_LETTER_CODES.contains(land)) {
			throw new ValidationException(format("Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=%s", land));
		}
	}

	public static void validateJournalpostAndDokumenter(Journalpost journalpost) {
		assertNotNull(Journalposttype.class, journalpost.getJournalposttype());
		assertParameterIsAsExpected("journalposttype", journalpost.getJournalposttype().name(), UTGAAENDE);
		assertParameterIsAsExpected("journalpoststatus", journalpost.getJournalstatus(), FERDIGSTILT);

		assertJournalpostFieldNotNull(Journalpost.Bruker.class, journalpost.getBruker());
		assertJournalpostFieldNotNullOrEmpty("brukerId", journalpost.getBruker().getId());
		assertJournalpostFieldNotNull(BrukerIdType.class, journalpost.getBruker().getType());

		assertJournalpostFieldNotNull(Journalpost.AvsenderMottaker.class, journalpost.getAvsenderMottaker());
		assertJournalpostFieldNotNullOrEmpty("mottakerNavn", journalpost.getAvsenderMottaker().getNavn());

		validateHovedDokumentInfo(journalpost.getDokumenter().iterator().next());

		journalpost.getDokumenter().forEach(Rdist002ValidationUtil::validateDokumentInfo);
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
				.noneMatch(dokumentvariant -> dokumentvariant.isSaksbehandlerHarTilgang() && (VARIANTFORMATS.contains(dokumentvariant.getVariantformat())));
	}

	private static void checkIfNoDokumentvariantWithFiltypePdfOrPdfA(List<Journalpost.Dokumentvariant> dokumentvarianter) {
		 dokumentvarianter.forEach(dokumentvariant -> {
			 if (!(VARIANTFORMATS.contains(dokumentvariant.getVariantformat()) && FILETYPE_PDF_PDFA.contains(dokumentvariant.getFiltype()))) {
				 throw new InvalidFiltypeException(format("Ugyldig dokumentvariant=%s eller filtype=%s, kun dokumentvariant ARKIV/SLADDET med filtype PDF/PDFA kan distribueres", dokumentvariant.getVariantformat(), dokumentvariant.getFiltype()));
			 }
		 });
	}
}
