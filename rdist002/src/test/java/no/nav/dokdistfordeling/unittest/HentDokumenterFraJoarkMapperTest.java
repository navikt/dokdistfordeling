package no.nav.dokdistfordeling.unittest;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.constants.ValidationConstants.ARKIV;
import static no.nav.dokdistfordeling.constants.ValidationConstants.SLADDET;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ADRESSELINJE1;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ADRESSELINJE2;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ADRESSELINJE3;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ARKIV_SYSTEM;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BATCH_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BESTILLENDEFAGSYSTEM;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.BRUKER_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.DOKUMENTPRODAPP;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.DOKUMENTTYPEID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.DOK_INFO_ID_1;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.DOK_INFO_ID_2;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.JOURNALPOST_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.LAND_NO;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.LAND_US;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.MOTTAKER_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.MOTTAKER_NAVN;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ORGNR;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.ORG_NAVN;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.POSTNUMMER;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.POSTSTED;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.SAMHANDLER_ID;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.SAMHANDLER_KATOGORI;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.SAMHANDLER_NAVN;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.TEMA;
import static no.nav.dokdistfordeling.unittest.UnitTestUtil.TITTEL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.nav.dokdistfordeling.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.HentDokumenterFraJoarkMapper;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.HentDokumenterFraJoark;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.UtenlandskPostadresse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class HentDokumenterFraJoarkMapperTest {

	private static final String BESTILLINGS_ID = "7cc280ce-4168-4204-8d03-8dbdc3c4fc32";

	private UnitTestUtil unitTestUtil = new UnitTestUtil();
	private HentDokumenterFraJoarkMapper mapper = new HentDokumenterFraJoarkMapper();

	@Test
	public void shouldMap() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				unitTestUtil.createJournalpostBuilder().build(),
				createPersonMottaker(),
				unitTestUtil.createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

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
		assertNorskPostadresse((NorskPostadresse) bestilling.getAdresse());
		assertEquals(DOKUMENTPRODAPP, bestilling.getDokumentProdApp());
		assertDokumenter(bestilling.getDokumenter());
	}

	@Test
	public void shouldMapWithUtenlandskAdresse() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder()
						.adresse(unitTestUtil.createUtenlandskPostadresse()).build(),
				unitTestUtil.createJournalpostBuilder().build(),
				createPersonMottaker(),
				unitTestUtil.createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertUtenlandskPostadresse((UtenlandskPostadresse) result.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapWithMottakerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				unitTestUtil.createJournalpostBuilder().build(),
				createOrganisasjonMottaker(),
				unitTestUtil.createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonMottaker((Organisasjon) result.getDistribusjonbestilling().getMottaker());
	}

	@Disabled
	@Test
	public void shouldMapWithMottakerAsSamhandler() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				unitTestUtil.createJournalpostBuilder().build(),
				createSamhandlerMottaker(),
				unitTestUtil.createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithBrukerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				unitTestUtil.createJournalpostBuilder()
						.bruker(unitTestUtil.createBrukerWithOrgnrId())
						.build(),
				createPersonMottaker(),
				unitTestUtil.createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonBruker((Organisasjon) result.getDistribusjonbestilling().getBruker());
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
			assertEquals(DOKUMENTTYPEID, dokument.getDokumenttypeId());

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

	private void assertNorskPostadresse(NorskPostadresse adresse) {
		assertEquals(LAND_NO, adresse.getLand());
		assertEquals(POSTNUMMER, adresse.getPostnummer());
		assertEquals(POSTSTED, adresse.getPoststed());
		assertEquals(ADRESSELINJE1, adresse.getAdresselinje1());
		assertNull(adresse.getAdresselinje2());
		assertNull(adresse.getAdresselinje3());
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
				.adresse(unitTestUtil.createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP);
	}

	private Person createPersonMottaker() {
		return new Person()
				.withPersonidentifikator(MOTTAKER_ID)
				.withNavn(MOTTAKER_NAVN);
	}

	private Organisasjon createOrganisasjonMottaker() {
		return new Organisasjon()
				.withOrgnummer(ORGNR)
				.withNavn(ORG_NAVN);
	}

	private Samhandler createSamhandlerMottaker() {
		return new Samhandler()
				.withSamhandleridentifikator(SAMHANDLER_ID)
				.withSamhandlerkategori(SAMHANDLER_KATOGORI)
				.withNavn(SAMHANDLER_NAVN);
	}

}
