package no.nav.dokdistfordeling.validate;

import no.nav.dokdistfordeling.exception.functional.ValidationException;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import no.nav.dokdistfordeling.to.DistribuerJournalpostRequestTo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.math.BigInteger.ONE;
import static no.nav.dokdistfordeling.TestData.FORSENDSELSE_METADATA;
import static no.nav.dokdistfordeling.TestData.createDistribuerJournalpostToBuilder;
import static no.nav.dokdistfordeling.validate.DistribuerJournalpostRequestValidator.validateDistribuerJournalpostRequest;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.codehaus.plexus.util.StringUtils.isBlank;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DistribuerJournalpostRequestValidatorTest {

	@Test
	void shouldValidateDistribuerJournalpostRequest() {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder().build();

		assertDoesNotThrow(() -> validateDistribuerJournalpostRequest(request));
	}

	@ParameterizedTest
	@EnumSource(DistribusjonstypeCode.class)
	void shouldValidateDistribusjonstype(DistribusjonstypeCode distribusjonstype) {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.distribusjonstype(distribusjonstype.name())
				.build();

		validateDistribuerJournalpostRequest(request);
	}

	@ParameterizedTest
	@EnumSource(DistribusjonstidspunktCode.class)
	void shouldValidateDistribusjonstidspunkt(DistribusjonstidspunktCode distribusjonstidspunkt) {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.distribusjonstidspunkt(distribusjonstidspunkt.name())
				.build();

		validateDistribuerJournalpostRequest(request);
	}

	@ParameterizedTest
	@EnumSource(TvingKanal.class)
	@NullSource
	void shouldValidateTvingKanal(TvingKanal tvingKanal) {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.tvingKanal(tvingKanal == null ? null : tvingKanal.name())
				.build();

		validateDistribuerJournalpostRequest(request);
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenJournalpostIdIsInvalid(String journalpostId) {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.journalpostId(journalpostId)
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet journalpostId må være et ikke-negativt heltall. Fikk journalpostId=%s", journalpostId);
	}



	static Stream<String> shouldThrowValidationExceptionWhenJournalpostIdIsInvalid() {
		return Stream.of(null, " ", "-1", "1.1", "1,1", "ikkeEttTall", BigInteger.valueOf(Long.MAX_VALUE).add(ONE).toString());
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenRequiredFieldIsEmpty(DistribuerJournalpostRequestTo request, String field) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet %s kan ikke være null eller tomt. Fikk %s= ", field, field);
	}

	private static Stream<Arguments> shouldThrowValidationExceptionWhenRequiredFieldIsEmpty() {
		return Stream.of(
				Arguments.of(createDistribuerJournalpostToBuilder().bestillendeFagsystem(" ").build(), "bestillendeFagsystem"),
				Arguments.of(createDistribuerJournalpostToBuilder().dokumentProdApp(" ").build(), "dokumentProdapp"),
				Arguments.of(createDistribuerJournalpostToBuilder().distribusjonstype(" ").build(), "distribusjonstype"),
				Arguments.of(createDistribuerJournalpostToBuilder().distribusjonstidspunkt(" ").build(), "distribusjonstidspunkt")

		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenRequiredFieldIsNull(DistribuerJournalpostRequestTo request, String field) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet %s kan ikke være null eller tomt. Fikk %s=null", field, field);
	}

	private static Stream<Arguments> shouldThrowValidationExceptionWhenRequiredFieldIsNull() {
		return Stream.of(
				Arguments.of(createDistribuerJournalpostToBuilder().bestillendeFagsystem(null).build(), "bestillendeFagsystem"),
				Arguments.of(createDistribuerJournalpostToBuilder().dokumentProdApp(null).build(), "dokumentProdapp"),
				Arguments.of(createDistribuerJournalpostToBuilder().distribusjonstype(null).build(), "distribusjonstype"),
				Arguments.of(createDistribuerJournalpostToBuilder().distribusjonstidspunkt(null).build(), "distribusjonstidspunkt")
		);
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenFieldValueIsTooLong(DistribuerJournalpostRequestTo request, String field) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet %s kan ikke være mer enn 20 tegn", field);
	}

	private static Stream<Arguments> shouldThrowValidationExceptionWhenFieldValueIsTooLong() {
		return Stream.of(
				Arguments.of(createDistribuerJournalpostToBuilder().bestillendeFagsystem("DenneTekstenEr21Tegn!").build(), "bestillendeFagsystem"),
				Arguments.of(createDistribuerJournalpostToBuilder().dokumentProdApp("DenneTekstenEr21Tegn!").build(), "dokumentProdapp")
		);
	}

	@Test
	void shouldThrowValidationExceptionWhenDistribusjonstypeIsInvalid() {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.distribusjonstype("Ugyldig")
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet distribusjonstype hadde en ugyldig verdi. Fikk distribusjonstype=Ugyldig. Gyldige verdier er %s",
						Arrays.toString(DistribusjonstypeCode.values()));
	}

	@Test
	void shouldThrowValidationExceptionWhenDistribusjonstidspunktIsInvalid() {
		DistribuerJournalpostRequestTo request = createDistribuerJournalpostToBuilder()
				.distribusjonstidspunkt("Ugyldig")
				.build();

		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("Feltet distribusjonstidspunkt hadde en ugyldig verdi. Fikk distribusjonstidspunkt=Ugyldig. Gyldige verdier er %s",
						Arrays.toString(DistribusjonstidspunktCode.values()));
	}

	@ParameterizedTest
	@MethodSource
	void shouldThrowValidationExceptionWhenEitherForsendelseMetaDataOrTypeIsGiven(DistribuerJournalpostRequestTo request) {
		assertThatExceptionOfType(ValidationException.class)
				.isThrownBy(() -> validateDistribuerJournalpostRequest(request))
				.withMessage("forsendelsesMetadata og forsendelsesMetadataType må enten begge være satt, eller begge være null med forsendelsesmetadata=%s, forsendelsesmetadataType=%s",
						isBlank(request.getForsendelseMetadata()) ? null : "****", request.getForsendelseMetadataType());
	}

	static Stream<Arguments> shouldThrowValidationExceptionWhenEitherForsendelseMetaDataOrTypeIsGiven() {
		return Stream.of(
				Arguments.of(createDistribuerJournalpostToBuilder().forsendelseMetadata(FORSENDSELSE_METADATA).build(), "forsendelseMetadata"),
				Arguments.of(createDistribuerJournalpostToBuilder().forsendelseMetadataType(ForsendelseMetadataType.DPO_ARKIVMELDING.name()).build(), "forsendelseMetadataType")
		);
	}
}