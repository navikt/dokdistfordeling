package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.TestData;
import no.nav.dokdistfordeling.kodeverk.TvingKanal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import static no.nav.dokdistfordeling.TestData.createAdresseToBuilder;
import static no.nav.dokdistfordeling.TestData.createDistribuerJournalpostToBuilder;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VIKTIG;
import static org.assertj.core.api.Assertions.assertThat;

class DistribuerJournalpostMapperTest {

	@Test
	void shouldMapDistribuerJournalpostRequest() {
		var request = createDistribuerJournalpostToBuilder().build();

		var result = DistribuerJournalpostMapper.map(request);

		assertThat(result.journalpostId()).isEqualTo(TestData.JOURNALPOST_ID);
		assertThat(result.batchId()).isEqualTo(TestData.BATCH_ID);
		assertThat(result.bestillendeFagsystem()).isEqualTo(TestData.BESTILLENDEFAGSYSTEM);
		assertThat(result.dokumentProdApp()).isEqualTo(TestData.DOKUMENTPRODAPP);
		assertThat(result.distribusjonstidspunkt()).isEqualTo(KJERNETID);
		assertThat(result.distribusjonstype()).isEqualTo(VIKTIG);

		assertThat(result.postadresse()).usingRecursiveComparison()
				.isEqualTo(request.getAdresse());
	}

	@Test
	void shouldMapWhenAdresseIsNull() {
		var request = createDistribuerJournalpostToBuilder()
				.adresse(null)
				.build();

		var result = DistribuerJournalpostMapper.map(request);

		assertThat(result.postadresse()).isNull();
	}

	@ParameterizedTest
	@EnumSource(TvingKanal.class)
	@NullSource
	void shouldMapTvingKanal(TvingKanal tvingKanal) {
		var request = createDistribuerJournalpostToBuilder()
				.tvingKanal(tvingKanal == null ? null : tvingKanal.name())
				.build();

		var result = DistribuerJournalpostMapper.map(request);

		assertThat(result.tvingKanal()).isEqualTo(tvingKanal);
	}

	@Test
	void shouldMapAdresselinjeToTrimmedValue() {
		var address = createAdresseToBuilder()
				.adresselinje1("  Adresselinje 1  ")
				.adresselinje2("  Adresselinje 2  ")
				.adresselinje3("  Adresselinje 3  ")
				.build();

		var request = createDistribuerJournalpostToBuilder()
				.adresse(address)
				.build();

		var result = DistribuerJournalpostMapper.map(request);

		assertThat(result.postadresse())
				.extracting("adresselinje1", "adresselinje2", "adresselinje3")
				.containsExactly("Adresselinje 1", "Adresselinje 2", "Adresselinje 3");
	}

	@Test
	void shouldMapAdresselinjeToNullWhenBlank() {
		var address = createAdresseToBuilder()
				.adresselinje1("  ")
				.adresselinje2("  ")
				.adresselinje3("  ")
				.build();

		var request = createDistribuerJournalpostToBuilder()
				.adresse(address)
				.build();

		var result = DistribuerJournalpostMapper.map(request);

		assertThat(result.postadresse())
				.extracting("adresselinje1", "adresselinje2", "adresselinje3")
				.containsExactly(null, null, null);
	}
}