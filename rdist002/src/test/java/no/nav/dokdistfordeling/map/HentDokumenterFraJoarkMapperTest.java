package no.nav.dokdistfordeling.map;

import no.nav.dokdistfordeling.domain.DistribuerJournalpost;
import no.nav.dokdistfordeling.domain.Postadresse;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static no.nav.dokdistfordeling.TestData.ADRESSELINJE1;
import static no.nav.dokdistfordeling.TestData.ADRESSELINJE2;
import static no.nav.dokdistfordeling.TestData.ADRESSELINJE3;
import static no.nav.dokdistfordeling.TestData.AKTOER_ID;
import static no.nav.dokdistfordeling.TestData.ARKIV_SYSTEM;
import static no.nav.dokdistfordeling.TestData.BATCH_ID;
import static no.nav.dokdistfordeling.TestData.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.TestData.BRUKER_ID;
import static no.nav.dokdistfordeling.TestData.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.TestData.DOK_INFO_ID_1;
import static no.nav.dokdistfordeling.TestData.DOK_INFO_ID_2;
import static no.nav.dokdistfordeling.TestData.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.TestData.LANDKODE_US;
import static no.nav.dokdistfordeling.TestData.MOTTAKER_ID;
import static no.nav.dokdistfordeling.TestData.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.TestData.ORGNR;
import static no.nav.dokdistfordeling.TestData.ORG_NAVN;
import static no.nav.dokdistfordeling.TestData.POSTNUMMER;
import static no.nav.dokdistfordeling.TestData.POSTSTED;
import static no.nav.dokdistfordeling.TestData.SAMHANDLER_ID;
import static no.nav.dokdistfordeling.TestData.SAMHANDLER_KATOGORI;
import static no.nav.dokdistfordeling.TestData.SAMHANDLER_NAVN;
import static no.nav.dokdistfordeling.TestData.TEMA;
import static no.nav.dokdistfordeling.TestData.TITTEL;
import static no.nav.dokdistfordeling.TestData.TSS_ID;
import static no.nav.dokdistfordeling.TestData.TSS_KATEGORI;
import static no.nav.dokdistfordeling.TestData.TSS_NAVN;
import static no.nav.dokdistfordeling.TestData.createBrukerWithAktoerId;
import static no.nav.dokdistfordeling.TestData.createBrukerWithOrgnrId;
import static no.nav.dokdistfordeling.TestData.createDistribuerJournalpostBuilder;
import static no.nav.dokdistfordeling.TestData.createJournalpostBuilder;
import static no.nav.dokdistfordeling.TestData.createNorskPostadresseBuilder;
import static no.nav.dokdistfordeling.TestData.createUtenlandskPostadresseBuilder;
import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.domain.Postadresse.LANDKODE_NORGE;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.SDP;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.UMIDDELBART;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.assertj.core.api.Assertions.assertThat;

public class HentDokumenterFraJoarkMapperTest {

	private static final String BESTILLINGS_ID = "7cc280ce-4168-4204-8d03-8dbdc3c4fc32";
	private static final Postadresse ADRESSE = createNorskPostadresseBuilder().build();

	@Test
	void shouldMap() {
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder().build();

		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				distribuerJournalpost,
				ADRESSE,
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		Distribusjonbestilling bestilling = result.getDistribusjonbestilling();

		assertThat(bestilling)
				.isNotNull()
				.satisfies(b -> {
					assertThat(b.getBestillingsId()).isEqualTo(BESTILLINGS_ID);
					assertThat(b.getBatchId()).isEqualTo(BATCH_ID);
					assertThat(b.getDistribusjonKanal()).isEqualTo(PRINT.name());
					assertThat(b.getBestillendeFagsystem()).isEqualTo(BESTILLENDEFAGSYSTEM);
					assertThat(b.getTema()).isEqualTo(TEMA);
					assertThat(b.getForsendelseTittel()).isEqualTo(TITTEL);
					assertThat(b.getDistribusjonstype()).isEqualTo(VEDTAK.name());
					assertThat(b.getDistribusjonstidspunkt()).isEqualTo(UMIDDELBART.name());
					assertThat(b.getDokumentProdApp()).isEqualTo(DOKUMENTPRODAPP);
				});

		assertArkivinformasjon(bestilling.getArkivInformasjon());
		assertPersonMottaker((Person) bestilling.getMottaker());
		assertPersonBruker((Person) bestilling.getBruker());
		assertNorskPostadresse((NorskPostadresse) bestilling.getAdresse());
		assertDokumenter(bestilling.getDokumenter());
	}

	@ParameterizedTest
	@EnumSource(DistribusjonstypeCode.class)
	void shouldMapDistribusjonstype(DistribusjonstypeCode distribusjonstype) {
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder()
				.distribusjonstype(distribusjonstype)
				.build();

		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				distribuerJournalpost,
				ADRESSE,
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling())
				.isNotNull()
				.extracting(Distribusjonbestilling::getDistribusjonstype)
				.isEqualTo(distribusjonstype.name());
	}

	@ParameterizedTest
	@EnumSource(DistribusjonstidspunktCode.class)
	void shouldMapDistribusjonstidspunkt(DistribusjonstidspunktCode distribusjonstidspunkt) {
		DistribuerJournalpost distribuerJournalpost = createDistribuerJournalpostBuilder()
				.distribusjonstidspunkt(distribusjonstidspunkt)
				.build();

		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				distribuerJournalpost,
				ADRESSE,
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling())
				.isNotNull()
				.extracting(Distribusjonbestilling::getDistribusjonstidspunkt)
				.isEqualTo(distribusjonstidspunkt.name());
	}

	@Test
	void shouldMapWhenAdresseIsNull() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				null,
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		Distribusjonbestilling bestilling = result.getDistribusjonbestilling();

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertThat(bestilling.getAdresse()).isNull();
	}

	@Test
	void shouldMapWithUtenlandskAdresse() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				createUtenlandskPostadresseBuilder().build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertUtenlandskPostadresse((UtenlandskPostadresse) result.getDistribusjonbestilling().getAdresse());
	}

	@Test
	void shouldMapWhenMottakerIsOrganisasjon() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				ADRESSE,
				createJournalpostBuilder().build(),
				createOrganisasjonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertOrganisasjonMottaker((Organisasjon) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	void shouldMapWhenMottakerIsSamhandler() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				ADRESSE,
				createJournalpostBuilder().build(),
				createSamhandlerMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	void shouldMapWhenMottakerIsTSS() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				ADRESSE,
				createJournalpostBuilder().build(),
				createTSSMottaker(),
				BESTILLINGS_ID,
				SDP);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertUKJENTSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	void shouldMapWhenBrukerIsOrganisasjon() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				ADRESSE,
				createJournalpostBuilder().bruker(createBrukerWithOrgnrId()).build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				DITTNAV);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertOrganisasjonBruker((Organisasjon) result.getDistribusjonbestilling().getBruker());
	}

	@Test
	void shouldMapWhenBrukerIsAktoerId() {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().build(),
				ADRESSE,
				createJournalpostBuilder().bruker(createBrukerWithAktoerId()).build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				DITTNAV);

		assertThat(result.getDistribusjonbestilling()).isNotNull();
		assertAktorIdBruker((AktoerId) result.getDistribusjonbestilling().getBruker());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	void shouldMapBatchIdToNullWhenBlank(String batchId) {
		HentDokumenterFraJoark result = HentDokumenterFraJoarkMapper.map(
				createDistribuerJournalpostBuilder().batchId(batchId).build(),
				ADRESSE,
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID,
				PRINT);

		assertThat(result.getDistribusjonbestilling())
				.isNotNull()
				.satisfies(b -> assertThat(b.getBatchId()).isNull());
	}

	private void assertDokumenter(List<DokumentInformasjon> dokumenter) {
		assertThat(dokumenter).hasSize(2);

		dokumenter.forEach(dokument -> {
			if (HOVEDDOKUMENT.name().equals(dokument.getTilknyttetSom())) {
				assertThat(dokument.getRekkefolge()).isEqualTo(1);
				assertThat(dokument.getVariantFormat()).isEqualTo(SLADDET);
				assertThat(dokument.getArkivDokumentInfoId()).isEqualTo(DOK_INFO_ID_1);
			} else {
				assertThat(dokument.getRekkefolge()).isGreaterThan(1);
				assertThat(dokument.getTilknyttetSom()).isEqualTo(VEDLEGG.name());
				assertThat(dokument.getVariantFormat()).isEqualTo(ARKIV);
				assertThat(dokument.getArkivDokumentInfoId()).isEqualTo(DOK_INFO_ID_2);
			}
			assertThat(dokument.getDokumenttypeId()).isEqualTo(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID);
		});
	}

	private void assertArkivinformasjon(ArkivInformasjon arkivInformasjon) {
		assertThat(arkivInformasjon.getArkivId()).isEqualTo(String.valueOf(JOURNALPOST_ID));
		assertThat(arkivInformasjon.getArkivSystem()).isEqualTo(ARKIV_SYSTEM);
	}

	private void assertAktorIdBruker(AktoerId aktoerId) {
		assertThat(aktoerId.getAktoerId()).isEqualTo(AKTOER_ID);
		assertThat(aktoerId.getNavn()).isNull();
	}


	private void assertPersonBruker(Person bruker) {
		assertThat(bruker.getPersonidentifikator()).isEqualTo(BRUKER_ID);
		assertThat(bruker.getNavn()).isNull();
	}

	private void assertOrganisasjonBruker(Organisasjon bruker) {
		assertThat(bruker.getOrgnummer()).isEqualTo(ORGNR);
		assertThat(bruker.getNavn()).isNull();
	}

	private void assertPersonMottaker(Person mottaker) {
		assertThat(mottaker.getPersonidentifikator()).isEqualTo(MOTTAKER_ID);
		assertThat(mottaker.getNavn()).isEqualTo(MOTTAKER_NAVN);
	}

	private void assertOrganisasjonMottaker(Organisasjon mottaker) {
		assertThat(mottaker.getOrgnummer()).isEqualTo(ORGNR);
		assertThat(mottaker.getNavn()).isEqualTo(ORG_NAVN);
	}

	private void assertSamhandlerMottaker(Samhandler mottaker) {
		assertThat(mottaker.getSamhandleridentifikator()).isEqualTo(SAMHANDLER_ID);
		assertThat(mottaker.getSamhandlerkategori()).isEqualTo(SAMHANDLER_KATOGORI);
		assertThat(mottaker.getNavn()).isEqualTo(SAMHANDLER_NAVN);
	}

	private void assertUKJENTSamhandlerMottaker(Samhandler mottaker) {
		assertThat(mottaker.getSamhandleridentifikator()).isEqualTo(TSS_ID);
		assertThat(mottaker.getSamhandlerkategori()).isEqualTo(TSS_KATEGORI);
		assertThat(mottaker.getNavn()).isEqualTo(TSS_NAVN);
	}

	private void assertNorskPostadresse(NorskPostadresse adresse) {
		assertThat(adresse.getLand()).isEqualTo(LANDKODE_NORGE);
		assertThat(adresse.getPostnummer()).isEqualTo(POSTNUMMER);
		assertThat(adresse.getPoststed()).isEqualTo(POSTSTED);
		assertThat(adresse.getAdresselinje1()).isEqualTo(ADRESSELINJE1);
		assertThat(adresse.getAdresselinje2()).isEqualTo(ADRESSELINJE2);
		assertThat(adresse.getAdresselinje3()).isEqualTo(ADRESSELINJE3);
	}

	private void assertUtenlandskPostadresse(UtenlandskPostadresse adresse) {
		assertThat(adresse.getLand()).isEqualTo(LANDKODE_US);
		assertThat(adresse.getAdresselinje1()).isEqualTo(ADRESSELINJE1);
		assertThat(adresse.getAdresselinje2()).isEqualTo(ADRESSELINJE2);
		assertThat(adresse.getAdresselinje3()).isEqualTo(ADRESSELINJE3);
	}

	private Person createPersonMottaker() {
		Person person = new Person();
		person.setPersonidentifikator(MOTTAKER_ID);
		person.setNavn(MOTTAKER_NAVN);
		return person;
	}

	private Organisasjon createOrganisasjonMottaker() {
		Organisasjon organisasjon = new Organisasjon();
		organisasjon.setOrgnummer(ORGNR);
		organisasjon.setNavn(ORG_NAVN);
		return organisasjon;
	}

	private Samhandler createSamhandlerMottaker() {
		Samhandler samhandler = new Samhandler();
		samhandler.setSamhandleridentifikator(SAMHANDLER_ID);
		samhandler.setSamhandlerkategori(SAMHANDLER_KATOGORI);
		samhandler.setNavn(SAMHANDLER_NAVN);
		return samhandler;
	}

	private Samhandler createTSSMottaker() {
		Samhandler samhandler = new Samhandler();
		samhandler.setSamhandleridentifikator(TSS_ID);
		samhandler.setSamhandlerkategori(TSS_KATEGORI);
		samhandler.setNavn(TSS_NAVN);
		return samhandler;
	}

}
