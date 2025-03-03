package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static no.nav.dokdistfordeling.TestData.MOTTAKER_ID;
import static no.nav.dokdistfordeling.TestData.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.TestData.createMottaker;
import static no.nav.dokdistfordeling.TestData.createNorskPostadresseBuilder;
import static no.nav.dokdistfordeling.TestData.createUtenlandskPostadresseBuilder;
import static no.nav.dokdistfordeling.validate.AdresseValidator.ISO3166_TWO_LETTER_CODES;
import static no.nav.dokdistfordeling.validate.AdresseValidator.KOSOVO_LAND_KODE;
import static no.nav.dokdistfordeling.validate.AdresseValidator.validateAdresse;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AdresseValidatorTest {

	private static final Person MOTTAKER = createMottaker();

	@Test
	void shouldValidateNorskPostadresse() {
		assertDoesNotThrow(() -> validateAdresse(createNorskPostadresseBuilder().build(), MOTTAKER));
	}

	@Test
	void shouldValidateUtenlandskPostadresse() {
		assertDoesNotThrow(() -> validateAdresse(createUtenlandskPostadresseBuilder().build(), MOTTAKER));
	}

	@Test
	void shouldValidateAdresseWhenAdresseIsNull() {
		assertDoesNotThrow(() -> validateAdresse(null, MOTTAKER));
	}

	@ParameterizedTest
	@MethodSource
	void shouldValidateLandkode(String landkode) {
		var adresse = createUtenlandskPostadresseBuilder()
				.land(landkode)
				.build();

		assertDoesNotThrow(() -> validateAdresse(adresse, MOTTAKER));
	}

	static Stream<String> shouldValidateLandkode() {
		return ISO3166_TWO_LETTER_CODES.stream();
	}
	
	@Test
	void shouldValidateKosovoLandkode() {
		assertDoesNotThrow(() -> validateAdresse(createUtenlandskPostadresseBuilder().land(KOSOVO_LAND_KODE).build(), MOTTAKER));
	}

	@Test
	void shouldThrowValidationExceptionFromSamhandlerWithoutAdresse() {
		Samhandler samhandler = new Samhandler();
		samhandler.setNavn(MOTTAKER_NAVN);
		samhandler.setSamhandleridentifikator(MOTTAKER_ID);

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(null, samhandler))
				.withMessage("For mottaker av type samhandler kan ikke adresse være null");
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", ""})
	@NullSource
	void shouldThrowValdationExceptionWhenPoststedForNorskPostadresseIsNullOrEmpty(String poststed) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createNorskPostadresseBuilder().poststed(poststed).build(), MOTTAKER))
				.withMessage("Feltet poststed kan ikke være null eller tomt. Fikk poststed=%s", poststed);
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", ""})
	@NullSource
	void shouldThrowValdationExceptionWhenPostnummerForNorskPostadresseIsNullOrEmpty(String postnummer) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createNorskPostadresseBuilder().postnummer(postnummer).build(), MOTTAKER))
				.withMessage("Feltet postnummer kan ikke være null eller tomt. Fikk postnummer=%s", postnummer);
	}

	@ParameterizedTest
	@ValueSource(strings = {"123", "12345", "abcd"})
	void shouldThrowValidationExceptionWhenPostnummerForNorskPostadresseIsInvalid(String postnummer) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createNorskPostadresseBuilder().postnummer(postnummer).build(), MOTTAKER))
				.withMessage("Feltet postnummer må være et gyldig tall med 4 siffer. Fikk postnummer=%s", postnummer);
	}

	@ParameterizedTest
	@ValueSource(strings = {" ", ""})
	@NullSource
	void shouldThrowValidationExceptionWhenUtenlandskAdresseIsMissingAdresselinje1(String adresselinje1) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createUtenlandskPostadresseBuilder().adresselinje1(adresselinje1).build(), MOTTAKER))
				.withMessage("Feltet adresselinje1 kan ikke være null eller tomt. Fikk adresselinje1=%s", adresselinje1);

	}

	@ParameterizedTest
	@CsvSource({
			"1337,",
			"1337,Sandvika",
			",Sandvika",
	})
	void shouldThrowValidationExceptionWhenUtenlandskAdresseHasPostnummerOrPostSted(String postnummer, String poststed) {

		var adresse = createUtenlandskPostadresseBuilder()
				.postnummer(postnummer)
				.poststed(poststed)
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(adresse, MOTTAKER))
				.withMessage("Feltene postnummer og poststed kan ikke være satt når adressetype=utenlandskPostadresse. Fikk postnummer=%s og poststed=%s", postnummer, poststed);
	}


	@ParameterizedTest
	@ValueSource(strings = {"NOR", "", " "})
	@NullSource
	void shouldThrowValidationExceptionWhenLandkodeIsInvalid(String landkode) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createUtenlandskPostadresseBuilder().land(landkode).build(), MOTTAKER))
				.withMessage("Land må være en gyldig iso3166-2 landkode på 2 bokstaver. Fikk=%s", landkode);
	}

	@ParameterizedTest
	@ValueSource(strings = {"intergalaktiskPostadresse", " ", ""})
	@NullSource
	void shouldThrowValidationExceptionForMissingAdresseType(String adresseType) {

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateAdresse(createNorskPostadresseBuilder().adressetype(adresseType).build(), MOTTAKER))
				.withMessage("AdresseType må være enten norskPostadresse eller utenlandskPostadresse, adresseType=%s", adresseType);
	}

}