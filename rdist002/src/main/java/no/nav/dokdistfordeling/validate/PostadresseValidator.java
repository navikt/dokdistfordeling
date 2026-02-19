package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.nav.dokdistfordeling.domain.Postadresse.NORSK_POSTADRESSE;
import static no.nav.dokdistfordeling.domain.Postadresse.UTENLANDSK_POSTADRESSE;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertNotNullOrEmpty;
import static no.nav.dokdistfordeling.util.ValidationUtil.assertStringIsNumberOfExactLength;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class PostadresseValidator {

	public static final Set<String> ISO3166_TWO_LETTER_CODES = Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());
	public static final String KOSOVO_LAND_KODE = "XK";

	static {
		ISO3166_TWO_LETTER_CODES.add(KOSOVO_LAND_KODE);
	}

	private PostadresseValidator() {
	}

	public static void validatePostadresse(Postadresse postadresse, Aktoer mottaker) {
		if (mottaker instanceof Samhandler && postadresse == null) {
			throw new ValidationException("For mottaker av type samhandler kan ikke postadresse være null");
		}

		if (postadresse == null) {
			return;
		}

		validateLandKode(postadresse.land());

		if (postadresse.erNorskPostadresse()) {
			assertNotNullOrEmpty("poststed", postadresse.poststed());
			assertNotNullOrEmpty("postnummer", postadresse.postnummer());
			assertStringIsNumberOfExactLength("postnummer", postadresse.postnummer().strip(), 4);

		} else if (postadresse.erUtenlandskPostadresse()) {
			assertNotNullOrEmpty("adresselinje1", postadresse.adresselinje1());
			if (isNotBlank(postadresse.postnummer()) || isNotBlank(postadresse.poststed())) {
				throw new ValidationException(format("Feltene postnummer og poststed kan ikke være satt når adressetype=%s. Fikk postnummer=%s og poststed=%s", UTENLANDSK_POSTADRESSE, postadresse.postnummer(), postadresse.poststed()));
			}
		} else {
			throw new ValidationException(format("AdresseType må være enten %s eller %s, adresseType=%s", NORSK_POSTADRESSE, UTENLANDSK_POSTADRESSE, postadresse.adressetype()));
		}
	}

	private static void validateLandKode(String land) {
		if (!ISO3166_TWO_LETTER_CODES.contains(land)) {
			throw new ValidationException(format("Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=%s", land));
		}
	}
}
