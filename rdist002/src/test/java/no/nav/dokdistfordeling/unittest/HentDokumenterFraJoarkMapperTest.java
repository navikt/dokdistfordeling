package no.nav.dokdistfordeling.unittest;

import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.HOVEDDOKUMENT;
import static no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode.VEDLEGG;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.ARKIV;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.PRODUKSJON;
import static no.nav.dokdistfordeling.kodeverk.Variantformat.SLADDET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.nav.dokdistfordeling.consumer.saf.journalpost.AvsenderMottaker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Bruker;
import no.nav.dokdistfordeling.consumer.saf.journalpost.DokumentInfo;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Dokumentvariant;
import no.nav.dokdistfordeling.consumer.saf.journalpost.Journalpost;
import no.nav.dokdistfordeling.endpoints.DistribuerJournalpostRequestTo;
import no.nav.dokdistfordeling.endpoints.HentDokumenterFraJoarkMapper;
import no.nav.dokdistfordeling.kodeverk.BrukerIdType;
import no.nav.dokdistfordeling.kodeverk.Dokumentstatus;
import no.nav.dokdistfordeling.kodeverk.Journalposttype;
import no.nav.dokdistfordeling.kodeverk.Journalstatus;
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

import java.util.Arrays;
import java.util.List;

public class HentDokumenterFraJoarkMapperTest {

	private static final String BESTILLINGS_ID = "7cc280ce-4168-4204-8d03-8dbdc3c4fc32";

	private static final String JOURNALPOST_ID = "555555555";
	private static final Journalposttype JOURNALPOST_TYPE = Journalposttype.U;
	private static final Journalstatus JP_FERDIGSTILT = Journalstatus.FERDIGSTILT;
	private static final String BATCH_ID = "66666";
	private static final String BESTILLENDEFAGSYSTEM = "bestillendeFagsystem";
	private static final String ADRESSETYPE_NORSK = "norskPostadresse";
	private static final String ADRESSETYPE_UTENLANDSK = "utenlandskPostadresse";
	private static final String ADRESSELINJE1 = "eksempelveien 23 A";
	private static final String ADRESSELINJE2 = "eksempelveien 24 A";
	private static final String ADRESSELINJE3 = "eksempelveien 25 A";
	private static final String POSTSTED = "poststed";
	private static final String POSTNUMMER = "1337";
	private static final String LAND_NO = "NO";
	private static final String LAND_US = "US";
	private static final String DOKUMENTPRODAPP = "dokumentprodapp";
	private static final String DOK_TITTEL_1 = "DOK_TITTEL_1";
	private static final String DOK_TITTEL_2 = "DOK_TITTEL_2";
	private static final String BREVKODE = "000001";

	private static final String DOKUMENTTYPEID = "000001";
	private static final String TITTEL = "journalpostTittel";
	private static final String TEMA = "OPP";
	private static final String MOTTAKER_ID = "***gammelt_fnr***";
	private static final String MOTTAKER_NAVN = "Jan Neimansen";
	private static final String BRUKER_ID = "***gammelt_fnr***";
	private static final String BRUKER_NAVN = "***gammelt_fnr***";
	private static final String ORGNR = "776677665";
	private static final String ORG_NAVN = "eksempelcorp ASA";
	private static final String SAMHANDLER_KATOGORI = "HPR";
	private static final String SAMHANDLER_NAVN = "Betina Samhandlerson";
	private static final String SAMHANDLER_ID = "33322211";
	private static final String DOK_INFO_ID_1 = "666666666";
	private static final String DOK_INFO_ID_2 = "777777777";

	private HentDokumenterFraJoarkMapper mapper = new HentDokumenterFraJoarkMapper();

	@Test
	public void shouldMap() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				createDefaultDokumentInfoList(),
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
						.adresse(createUtenlandskPostadresse()).build(),
				createJournalpostBuilder().build(),
				createPersonMottaker(),
				createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertUtenlandskPostadresse((UtenlandskPostadresse) result.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapWithMottakerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createOrganisasjonMottaker(),
				createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonMottaker((Organisasjon) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithMottakerAsSamhandler() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder().build(),
				createSamhandlerMottaker(),
				createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertSamhandlerMottaker((Samhandler) result.getDistribusjonbestilling().getMottaker());
	}

	@Test
	public void shouldMapWithBrukerAsOrganisasjon() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder()
						.bruker(createBrukerWithOrgnrId())
						.build(),
				createPersonMottaker(),
				createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertOrganisasjonBruker((Organisasjon) result.getDistribusjonbestilling().getBruker());
	}

	@Test
	public void shouldMapWithBrukerAsSamhandler() {
		HentDokumenterFraJoark result = mapper.map(createDistribuerJournalpostRequestToBuilder().build(),
				createJournalpostBuilder()
						.bruker(createBrukerWithSamhandlerId())
						.build(),
				createPersonMottaker(),
				createDefaultDokumentInfoList(),
				BESTILLINGS_ID);

		assertNotNull(result.getDistribusjonbestilling());
		assertSamhandlerBruker((Samhandler) result.getDistribusjonbestilling().getBruker());
	}

	private void assertDokumenter(List<DokumentInformasjon> dokumenter) {
		assertEquals(2, dokumenter.size());
		dokumenter.forEach(dokument -> {
			if (HOVEDDOKUMENT.name().equals(dokument.getTilknyttetSom())) {
				assertEquals(dokument.getRekkefolge(), 1);
				assertEquals(SLADDET.name(), dokument.getVariantFormat());
				assertEquals(DOK_INFO_ID_1, dokument.getArkivDokumentInfoId());

			} else {
				assertThat(dokument.getRekkefolge(), greaterThan(1));
				assertEquals(VEDLEGG.name(), dokument.getTilknyttetSom());
				assertEquals(ARKIV.name(), dokument.getVariantFormat());
				assertEquals(DOK_INFO_ID_2, dokument.getArkivDokumentInfoId());
			}
			assertEquals(DOKUMENTTYPEID, dokument.getDokumenttypeId());

		});
	}

	private void assertArkivinformasjon(ArkivInformasjon arkivInformasjon) {
		assertEquals(JOURNALPOST_ID, arkivInformasjon.getArkivId());
		assertEquals(TEMA, arkivInformasjon.getArkivSystem());
	}

	private void assertPersonBruker(Person bruker) {
		assertEquals(BRUKER_NAVN, bruker.getPersonidentifikator());
		assertNull(bruker.getNavn());
//		assertEquals(BRUKER_NAVN, bruker.getNavn());
	}

	private void assertOrganisasjonBruker(Organisasjon bruker) {
		assertEquals(ORGNR, bruker.getOrgnummer());
		assertNull(bruker.getNavn());
//		assertEquals(ORG_NAVN, bruker.getNavn());
	}

	private void assertSamhandlerBruker(Samhandler bruker) {
		assertEquals(SAMHANDLER_ID, bruker.getSamhandleridentifikator());
		assertEquals(SAMHANDLER_KATOGORI, bruker.getSamhandlerkategori());
		assertNull(bruker.getNavn());
//		assertEquals(SAMHANDLER_NAVN, bruker.getNavn());
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
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENTPRODAPP);
	}

	private DistribuerJournalpostRequestTo.AdresseTo createNorskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_NORSK,
				POSTNUMMER,
				POSTSTED,
				ADRESSELINJE1,
				null,
				null,
				LAND_NO
		);
	}

	private DistribuerJournalpostRequestTo.AdresseTo createUtenlandskPostadresse() {
		return new DistribuerJournalpostRequestTo.AdresseTo(
				ADRESSETYPE_UTENLANDSK,
				null,
				null,
				ADRESSELINJE1,
				ADRESSELINJE2,
				ADRESSELINJE3,
				LAND_US
		);
	}

	private Journalpost.JournalpostBuilder createJournalpostBuilder() {
		return Journalpost.builder()
				.journalposttype(JOURNALPOST_TYPE)
				.journalstatus(JP_FERDIGSTILT)
				.tema(TEMA)
				.tittel(TITTEL)
				.bruker(createBrukerWithFNR())
				.avsenderMottaker(createAvsenderMottaker())
				.dokumenter(createDefaultDokumentInfoList());
	}

	private AvsenderMottaker createAvsenderMottaker() {
		return AvsenderMottaker.builder()
				.id(MOTTAKER_ID)
				.navn(MOTTAKER_NAVN)
				.build();
	}

	private Bruker createBrukerWithFNR() {
		return new Bruker(BRUKER_NAVN, BrukerIdType.FNR);
	}

	private Bruker createBrukerWithOrgnrId() {
		return new Bruker(ORGNR, BrukerIdType.ORGNR);
	}

	private Bruker createBrukerWithSamhandlerId() {
		return new Bruker(SAMHANDLER_ID, BrukerIdType.AKTOERID);
	}

	private List<DokumentInfo> createDefaultDokumentInfoList() {
		return Arrays.asList(
				createDokumentInfo1Builder().build(),
				createDokumentInfo2Builder().build());
	}

	private DokumentInfo.DokumentInfoBuilder createDokumentInfo1Builder() {
		return DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_1)
				.tittel(DOK_TITTEL_1)
				.brevkode(BREVKODE)
				.dokumentstatus(Dokumentstatus.FERDIGSTILT)
				.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
								.saksbehandlerHarTilgang(false)
								.variantformat(ARKIV).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(SLADDET).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(PRODUKSJON).build()));
	}

	private DokumentInfo.DokumentInfoBuilder createDokumentInfo2Builder() {
		return DokumentInfo.builder()
				.dokumentInfoId(DOK_INFO_ID_2)
				.tittel(DOK_TITTEL_2)
				.brevkode(BREVKODE)
				.dokumentvarianter(Arrays.asList(Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(ARKIV).build(),
						Dokumentvariant.builder()
								.saksbehandlerHarTilgang(true)
								.variantformat(PRODUKSJON).build()));
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
