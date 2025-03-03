package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType;
import no.nav.dokdistfordeling.kodeverk.SamhandlerKategoriCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static no.nav.dokdistfordeling.TestData.MOTTAKER_ID;
import static no.nav.dokdistfordeling.TestData.createAvsenderMottakerBuilder;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.FNR;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.HPRNR;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.ORGNR;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AvsenderMottakerIdType.UTL_ORG;
import static org.assertj.core.api.Assertions.assertThat;

class MottakerMapperTest {

	@Test
	void shouldMapPerson() {
		var avsenderMottaker = createAvsenderMottakerBuilder()
				.type(FNR)
				.build();

		Aktoer aktoer = MottakerMapper.map(avsenderMottaker);

		assertThat(aktoer)
				.isInstanceOf(Person.class)
				.extracting("navn", "personidentifikator")
				.containsExactly(avsenderMottaker.getNavn(), avsenderMottaker.getId());
	}

	@Test
	void shouldMapOrganisasjon() {
		var avsenderMottaker = createAvsenderMottakerBuilder()
				.type(ORGNR)
				.build();

		Aktoer aktoer = MottakerMapper.map(avsenderMottaker);

		assertThat(aktoer)
				.isInstanceOf(Organisasjon.class)
				.extracting("navn", "orgnummer")
				.containsExactly(avsenderMottaker.getNavn(), avsenderMottaker.getId());
	}

	@ParameterizedTest
	@MethodSource
	void shouldMapToSamhandler(String mottakerId,
							   AvsenderMottakerIdType type,
							   String forventetSamhandlerIdentifikator,
							   SamhandlerKategoriCode forventetSamhandlerKategori) {
		var avsenderMottaker = createAvsenderMottakerBuilder()
				.id(mottakerId)
				.type(type)
				.build();

		Aktoer aktoer = MottakerMapper.map(avsenderMottaker);

		assertThat(aktoer)
				.isInstanceOf(Samhandler.class)
				.extracting("navn", "samhandleridentifikator", "samhandlerkategori")
				.containsExactly(avsenderMottaker.getNavn(), forventetSamhandlerIdentifikator, forventetSamhandlerKategori.name());
	}

	static Stream<Arguments> shouldMapToSamhandler() {
			return Stream.of(
					//HPRNR
					Arguments.of(MOTTAKER_ID, HPRNR, MOTTAKER_ID, SamhandlerKategoriCode.HPR),
					Arguments.of("", HPRNR, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.HPR),
					Arguments.of(null, HPRNR, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.HPR),

					//UTL_ORG
					Arguments.of(MOTTAKER_ID, UTL_ORG, MOTTAKER_ID, SamhandlerKategoriCode.UTL_ORG),
					Arguments.of("", UTL_ORG, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UTL_ORG),
					Arguments.of(null, UTL_ORG, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UTL_ORG),

					//UKJENT
					Arguments.of(MOTTAKER_ID, UKJENT, MOTTAKER_ID, SamhandlerKategoriCode.UKJENT),
					Arguments.of("", UKJENT, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UKJENT),
					Arguments.of(null, UKJENT, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UKJENT),

					//null
					Arguments.of(MOTTAKER_ID, null, MOTTAKER_ID, SamhandlerKategoriCode.UKJENT),
					Arguments.of("", null, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UKJENT),
					Arguments.of(null, null, SamhandlerKategoriCode.UKJENT.name(), SamhandlerKategoriCode.UKJENT)
			);
	}
}