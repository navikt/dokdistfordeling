package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.domain.Adresse;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.map.HentDokumenterFraJoarkMapper.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertStringIsNumberOfExactLength;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class AdresseValidator {

	public static final Set<String> ISO3166_TWO_LETTER_CODES = Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());
	public static final String KOSOVO_LAND_KODE = "XK";

	static {
		ISO3166_TWO_LETTER_CODES.add(KOSOVO_LAND_KODE);
	}

	private AdresseValidator() {
	}

	public static void validateAdresse(Adresse adresse, Aktoer mottaker) {
		if (mottaker instanceof Samhandler && adresse == null) {
			throw new ValidationException("For mottaker av type samhandler kan ikke adresse være null");
		}

		if (adresse == null) {
			return;
		}

		validateLandKode(adresse.land());

		if (NORSK_POSTADRESSE.equals(adresse.adressetype())) {
			assertNotNullOrEmpty("poststed", adresse.poststed());
			assertNotNullOrEmpty("postnummer", adresse.postnummer());
			assertStringIsNumberOfExactLength("postnummer", adresse.postnummer().strip(), 4);

		} else if (UTENLANDSK_POSTADRESSE.equals(adresse.adressetype())) {
			assertNotNullOrEmpty("adresselinje1", adresse.adresselinje1());
			if (isNotBlank(adresse.postnummer()) || isNotBlank(adresse.poststed())) {
				throw new ValidationException(format("Feltene postnummer og poststed kan ikke være satt når adressetype=%s. Fikk postnummer=%s og poststed=%s", UTENLANDSK_POSTADRESSE, adresse.postnummer(), adresse.poststed()));
			}
		} else {
			throw new ValidationException(format("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType=%s", adresse.adressetype()));
		}
	}

	private static void validateLandKode(String land) {
		if (!ISO3166_TWO_LETTER_CODES.contains(land)) {
			throw new ValidationException(format("Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=%s", land));
		}
	}
}
