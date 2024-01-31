package no.nav.dokdistfordeling;

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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE1;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE2;
import static no.nav.dokdistfordeling.UnitTestUtil.ADRESSELINJE3;
import static no.nav.dokdistfordeling.UnitTestUtil.ARKIV_SYSTEM;
import static no.nav.dokdistfordeling.UnitTestUtil.BATCH_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.UnitTestUtil.BRUKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.UnitTestUtil.DOK_INFO_ID_1;
import static no.nav.dokdistfordeling.UnitTestUtil.DOK_INFO_ID_2;
import static no.nav.dokdistfordeling.UnitTestUtil.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.LAND_NO;
import static no.nav.dokdistfordeling.UnitTestUtil.LAND_US;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.ORGNR;
import static no.nav.dokdistfordeling.UnitTestUtil.ORG_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTNUMMER;
import static no.nav.dokdistfordeling.UnitTestUtil.POSTSTED;
import static no.nav.dokdistfordeling.UnitTestUtil.SAMHANDLER_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.SAMHANDLER_KATOGORI;
import static no.nav.dokdistfordeling.UnitTestUtil.SAMHANDLER_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.TEMA;
import static no.nav.dokdistfordeling.UnitTestUtil.TITTEL;
import static no.nav.dokdistfordeling.UnitTestUtil.TSS_ID;
import static no.nav.dokdistfordeling.UnitTestUtil.TSS_KATEGORI;
import static no.nav.dokdistfordeling.UnitTestUtil.TSS_NAVN;
import static no.nav.dokdistfordeling.UnitTestUtil.createBrukerWithOrgnrId;
import static no.nav.dokdistfordeling.UnitTestUtil.createJournalpostBuilder;
import static no.nav.dokdistfordeling.UnitTestUtil.createNorskPostadresse;
import static no.nav.dokdistfordeling.UnitTestUtil.createUtenlandskPostadresse;
import static no.nav.dokdistfordeling.constants.Constants.DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.SDP;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.UMIDDELBART;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HentDokumenterFraJoarkMapperTest {

	private static final String BESTILLINGS_ID = "7cc280ce-4168-4204-8d03-8dbdc3c4fc32";

	private HentDokumenterFraJoarkMapper mapper = new HentDokumenterFraJoarkMapper();

	@Test
	public void shouldMap() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder()
						.distribusjonstype(VEDTAK.name())
						.distribusjonstidspunkt(UMIDDELBART.name())
						.build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNotNull(result.getDistribusjonbestilling());
		Distribusjonbestilling bestilling = result.getDistribusjonbestilling();

		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsId());
		assertEquals(BATCH_ID, bestilling.getBatchId());
		assertEquals(BESTILLENDEFAGSYSTEM, bestilling.getBestillendeFagsystem());
		assertEquals(TEMA, bestilling.getTema());
		assertEquals(TITTEL, bestilling.getForsendelseTittel());
		assertEquals(bestilling.getDistribusjonstidspunkt(), UMIDDELBART.name());
		assertEquals(bestilling.getDistribusjonstype(), VEDTAK.name());

		assertArkivinformasjon(bestilling.getArkivInformasjon());
		assertPersonMottaker((Person) bestilling.getMottaker());
		assertPersonBruker((Person) bestilling.getBruker());
		assertNorskPostadresse((NorskPostadresse) bestilling.getAdresse());
		assertEquals(DOKUMENTPRODAPP, bestilling.getDokumentProdApp());
		assertDokumenter(bestilling.getDokumenter());
	}

	@Test
	public void shouldMapWhenAdresseIsNullAndKanalKodeSDP() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().adresse(null).build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID, SDP);

		assertNotNull(result.getDistribusjonbestilling());
		Distribusjonbestilling bestilling = result.getDistribusjonbestilling();

		assertEquals(BESTILLINGS_ID, bestilling.getBestillingsId());
		assertEquals(BATCH_ID, bestilling.getBatchId());
		assertEquals(BESTILLENDEFAGSYSTEM, bestilling.getBestillendeFagsystem());
		assertEquals(TEMA, bestilling.getTema());
		assertEquals(TITTEL, bestilling.getForsendelseTittel());

		assertArkivinformasjon(bestilling.getArkivInformasjon());
		assertPersonMottaker((Person) bestilling.getMottaker());
		assertPersonBruker((Person) bestilling.getBruker());
		assertNull(bestilling.getAdresse());
		assertEquals(DOKUMENTPRODAPP, bestilling.getDokumentProdApp());
		assertDokumenter(bestilling.getDokumenter());
	}

	@Test
	public void shouldMapWithUtenlandskAdresse() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder()
						.adresse(createUtenlandskPostadresse()).build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNotNull(result.getDistribusjonbestilling());
		assertUtenlandskPostadresse((UtenlandskPostadresse) result.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapWithMottakerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createOrganisasjonMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonMottaker((Organisasjon) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithMottakerAsSamhandler() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createSamhandlerMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNotNull(result.getDistribusjonbestilling());
		assertSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithMottakerAsTSS() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createTSSMottaker(),
				BESTILLINGS_ID, SDP);

		assertNotNull(result.getDistribusjonbestilling());
		assertUKJENTSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithBrukerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder()
						.bruker(createBrukerWithOrgnrId())
						.build(),
				createPersonMottaker(),
				BESTILLINGS_ID, DITTNAV);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonBruker((Organisasjon) result.getDistribusjonbestilling().getBruker());
	}

	@Test
	public void shouldSetNullAdresseWhenDistribusjonKanalKodeErPrint() {
		HentDokumenterFraJoark hentDokumenterFraJoark = mapper.map(createDistribuerJournalpostRequestToBuilder().adresse(null).build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNull(hentDokumenterFraJoark.getDistribusjonbestilling().getAdresse());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	public void shouldMapBatchIdToNullWhenBlank(String batchId) {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder()
						.distribusjonstype(VEDTAK.name())
						.distribusjonstidspunkt(UMIDDELBART.name())
						.batchId(batchId)
						.build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				BESTILLINGS_ID, PRINT);

		assertNotNull(result.getDistribusjonbestilling());
		Distribusjonbestilling bestilling = result.getDistribusjonbestilling();

		assertNull(bestilling.getBatchId());
	}

	private void assertDokumenter(List<DokumentInformasjon> dokumenter) {
		assertEquals(2, dokumenter.size());
		dokumenter.forEach(dokument -> {
			if (HOVEDDOKUMENT.name().equals(dokument.getTilknyttetSom())) {
				assertEquals(dokument.getRekkefolge(), 1);
				assertEquals(SLADDET, dokument.getVariantFormat());
				assertEquals(DOK_INFO_ID_1, dokument.getArkivDokumentInfoId());

			} else {
				assertThat(dokument.getRekkefolge(), greaterThan(1));
				assertEquals(VEDLEGG.name(), dokument.getTilknyttetSom());
				assertEquals(ARKIV, dokument.getVariantFormat());
				assertEquals(DOK_INFO_ID_2, dokument.getArkivDokumentInfoId());
			}
			assertEquals(DEFAULT_UTGAAENDE_DOKUMENTTYPE_ID, dokument.getDokumenttypeId());

		});
	}

	private void assertArkivinformasjon(ArkivInformasjon arkivInformasjon) {
		assertEquals(JOURNALPOST_ID, arkivInformasjon.getArkivId());
		assertEquals(ARKIV_SYSTEM, arkivInformasjon.getArkivSystem());
	}

	private void assertPersonBruker(Person bruker) {
		assertEquals(BRUKER_ID, bruker.getPersonidentifikator());
		assertNull(bruker.getNavn());
	}

	private void assertOrganisasjonBruker(Organisasjon bruker) {
		assertEquals(ORGNR, bruker.getOrgnummer());
		assertNull(bruker.getNavn());
	}

	private void assertSamhandlerBruker(Samhandler bruker) {
		assertEquals(SAMHANDLER_ID, bruker.getSamhandleridentifikator());
		assertEquals(SAMHANDLER_KATOGORI, bruker.getSamhandlerkategori());
		assertNull(bruker.getNavn());
	}

	private void assertPersonMottaker(Person mottaker) {
		assertEquals(MOTTAKER_ID, mottaker.getPersonidentifikator());
		assertEquals(MOTTAKER_NAVN, mottaker.getNavn());
	}

	private void assertOrganisasjonMottaker(Organisasjon mottaker) {
		assertEquals(ORGNR, mottaker.getOrgnummer());
		assertEquals(ORG_NAVN, mottaker.getNavn());
	}

	private void assertSamhandlerMottaker(Samhandler mottaker) {
		assertEquals(SAMHANDLER_ID, mottaker.getSamhandleridentifikator());
		assertEquals(SAMHANDLER_KATOGORI, mottaker.getSamhandlerkategori());
		assertEquals(SAMHANDLER_NAVN, mottaker.getNavn());
	}

	private void assertUKJENTSamhandlerMottaker(Samhandler mottaker) {
		assertEquals(TSS_ID, mottaker.getSamhandleridentifikator());
		assertEquals(TSS_KATEGORI, mottaker.getSamhandlerkategori());
		assertEquals(TSS_NAVN, mottaker.getNavn());
	}

	private void assertNorskPostadresse(NorskPostadresse adresse) {
		assertEquals(LAND_NO, adresse.getLand());
		assertEquals(POSTNUMMER, adresse.getPostnummer());
		assertEquals(POSTSTED, adresse.getPoststed());
		assertEquals(ADRESSELINJE1, adresse.getAdresselinje1());
		assertEquals(ADRESSELINJE2, adresse.getAdresselinje2());
		assertEquals(ADRESSELINJE3, adresse.getAdresselinje3());
	}

	private void assertUtenlandskPostadresse(UtenlandskPostadresse adresse) {
		assertEquals(LAND_US, adresse.getLand());
		assertEquals(ADRESSELINJE1, adresse.getAdresselinje1());
		assertEquals(ADRESSELINJE2, adresse.getAdresselinje2());
		assertEquals(ADRESSELINJE3, adresse.getAdresselinje3());
	}

	private DistribuerJournalpostRequestTo.DistribuerJournalpostRequestToBuilder createDistribuerJournalpostRequestToBuilder() {
		return DistribuerJournalpostRequestTo.builder()
				.journalpostId(JOURNALPOST_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDEFAGSYSTEM)
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP);
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
