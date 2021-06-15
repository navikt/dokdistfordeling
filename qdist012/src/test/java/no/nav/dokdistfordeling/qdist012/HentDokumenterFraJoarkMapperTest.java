package no.nav.dokdistfordeling.qdist012;

import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist012.Aktoer;
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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting.
 */

class HentDokumenterFraJoarkMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String TEMA = "DAG";
	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String ARKIV_SYSTEM = "JOARK";
	private static final String ARKIV_ID = "arkivId";
	private static final String PERSON_NAVN_MOTTAKER = "personNavnMottaker";
	private static final String PERSON_NAVN_BRUKER = "personNavnBruker";
	private static final String MOTTAKER_ID_NAVN = "mottakerIdNavn";
	private static final String ORGANISASJON_NAVN = "organisasjonNavn";
	private static final String SAMHANDLER_NAVN = "samhandlerNavn";
	private static final String PERSON_IDENTIFIKATOR_MOTTAKER = "personIdMottaker";
	private static final String PERSON_IDENTIFIKATOR_BRUKER = "personIdBruker";
	private static final String MOTTAKER_ID = "mottakerId";
	private static final String ORGNUMMER = "orgnr";
	private static final String SAMHANDLER_IDENTIFIKATOR = "samhandlerId";
	private static final String SAMHANDLER_KATEGORI_HPR = "HPR";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND = "land";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "dokumenttypeId1";
	private static final String DOKUMENTTYPE_ID_2 = "dokumenttypeId2";
	private static final String VARIANTFORMAT_1 = "variantformat1";
	private static final String VARIANTFORMAT_2 = "variantformat2";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";
	private static final int REKKEFOLGE_1 = 1;
	private static final int REKKEFOLGE_2 = 2;

	private final HentDokumenterFraJoarkMapper hentDokumenterFraJoarkMapper = new HentDokumenterFraJoarkMapper();


	@Test
	public void shouldMap() {
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(createHentDokumentFraJoark());
		assertResponse(hentDokumenterFraJoarkTo);
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().withAdresse(createUtenlandskPostadresse());
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertUtenlandskPostadresseTo(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapAktoerId() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().withMottaker(createMottakerAktoerId());
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), MOTTAKER_ID);
		assertEquals(mottakerTo.getNavn(), MOTTAKER_ID_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.PERSON);
		assertTrue(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOrganisasjon() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().withMottaker(createAktoerOrganisasjon());
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getBruker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), ORGNUMMER);
		assertEquals(mottakerTo.getNavn(), ORGANISASJON_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.ORGANISASJON);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapSamhandlerHpr() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().withMottaker(createAktoerSamhandlerHpr());
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.SAMHANDLER_HPR);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOkWhenWithoutAkivinformasjon() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().withArkivInformasjon(null);
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldMapOkWhenWithoutBatchId() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setBatchId(null);
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getBatchId());
	}

	@Test
	public void shouldMapOkWhenWithoutForsendelseTittel() {
		HentDokumenterFraJoark hentDokumenterFraJoark = createHentDokumentFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setForsendelseTittel(null);
		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getForsendelseTittel());
	}

	private void assertResponse(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		assertHentDokumentFraJoarkTo(hentDokumenterFraJoarkTo);

		//assert HentDokumenterFraJoarkTo
		final HentDokumenterFraJoarkTo.DistribusjonbestillingTo distBestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		assertEquals(distBestilling.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(distBestilling.getBatchId(), BATCH_ID);
		assertEquals(distBestilling.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(distBestilling.getTema(), TEMA);
		assertEquals(distBestilling.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(distBestilling.getDokumentProdApp(), DOKUMENT_PROD_APP);

		//assert Arkivinformasjon
		assertNotNull(distBestilling.getArkivInformasjon());
		assertEquals(distBestilling.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(distBestilling.getArkivInformasjon().getArkivSystem(), ARKIV_SYSTEM);

		//assert mottaker Person
		assertNotNull(distBestilling.getMottaker());
		assertEquals(distBestilling.getMottaker().getIdentifikator(), PERSON_IDENTIFIKATOR_MOTTAKER);
		assertEquals(distBestilling.getMottaker().getNavn(), PERSON_NAVN_MOTTAKER);
		assertEquals(distBestilling.getMottaker().getAktoerType(), AktoerTypeCode.PERSON);
		assertFalse(distBestilling.getMottaker().isIdentifikatorAktoerId());

		//assert bruker Person
		assertNotNull(distBestilling.getBruker());
		assertEquals(distBestilling.getBruker().getIdentifikator(), PERSON_IDENTIFIKATOR_BRUKER);
		assertEquals(distBestilling.getBruker().getNavn(), PERSON_NAVN_BRUKER);
		assertEquals(distBestilling.getBruker().getAktoerType(), AktoerTypeCode.PERSON);
		assertFalse(distBestilling.getBruker().isIdentifikatorAktoerId());


		//assert norsk postadresse
		assertNorskPostadresseTo(distBestilling.getAdresse());

		//assert dokumenter
		Assertions.assertThat(distBestilling.getDokumenter())
				.extracting(HentDokumenterFraJoarkTo.DokumentInformasjonTo::getDokumenttypeId,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getDokumentObjektReferanse,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getTilknyttetSom,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getArkivDokumentInfoId,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getRekkefolge,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getVariantFormat)
				.hasSize(2)
				.containsExactlyInAnyOrder(tuple(DOKUMENTTYPE_ID_1, null, TILKNYTTET_SOM_HOVEDDOK, ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1, VARIANTFORMAT_1),
						tuple(DOKUMENTTYPE_ID_2, null, TILKNYTTET_SOM_VEDLEGG, ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2, VARIANTFORMAT_2));
	}

	private void assertHentDokumentFraJoarkTo(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
	}

	private void assertNorskPostadresseTo(HentDokumenterFraJoarkTo.AdresseTo adresse) {
		assertTrue(adresse instanceof HentDokumenterFraJoarkTo.NorskPostadresseTo);
		final HentDokumenterFraJoarkTo.NorskPostadresseTo postadresse = (HentDokumenterFraJoarkTo.NorskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLand(), LAND);
	}

	private void assertUtenlandskPostadresseTo(HentDokumenterFraJoarkTo.AdresseTo adresse) {
		assertTrue(adresse instanceof HentDokumenterFraJoarkTo.UtenlandskPostadresseTo);
		final HentDokumenterFraJoarkTo.UtenlandskPostadresseTo postadresse = (HentDokumenterFraJoarkTo.UtenlandskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getLand(), LAND);
	}

	private HentDokumenterFraJoark createHentDokumentFraJoark() {
		return new HentDokumenterFraJoark()
				.withDistribusjonbestilling(new Distribusjonbestilling()
						.withBestillingsId(BESTILLINGS_ID)
						.withBatchId(BATCH_ID)
						.withBestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.withTema(TEMA)
						.withForsendelseTittel(FORSENDELSE_TITTEL)
						.withArkivInformasjon(new ArkivInformasjon()
								.withArkivId(ARKIV_ID)
								.withArkivSystem(ARKIV_SYSTEM))
						.withMottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER))
						.withBruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER))
						.withAdresse(createNorskPostadresse())
						.withDokumentProdApp(DOKUMENT_PROD_APP)
						.withDokumenter(Arrays.asList(new DokumentInformasjon()
										.withDokumenttypeId(DOKUMENTTYPE_ID_1)
										.withVariantFormat(VARIANTFORMAT_1)
										.withTilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
										.withArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
										.withRekkefolge(REKKEFOLGE_1),
								new DokumentInformasjon()
										.withDokumenttypeId(DOKUMENTTYPE_ID_2)
										.withVariantFormat(VARIANTFORMAT_2)
										.withTilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
										.withArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
										.withRekkefolge(REKKEFOLGE_2)
						))
				);
	}

	private NorskPostadresse createNorskPostadresse() {
		return new NorskPostadresse()
				.withAdresselinje1(ADRESSELINJE_1)
				.withAdresselinje2(ADRESSELINJE_2)
				.withAdresselinje3(ADRESSELINJE_3)
				.withPostnummer(POSTNUMMER)
				.withPoststed(POSTSTED)
				.withLand(LAND);
	}

	private UtenlandskPostadresse createUtenlandskPostadresse() {
		return new UtenlandskPostadresse()
				.withAdresselinje1(ADRESSELINJE_1)
				.withAdresselinje2(ADRESSELINJE_2)
				.withAdresselinje3(ADRESSELINJE_3)
				.withLand(LAND);
	}

	private Aktoer createAktoerPerson(String navn, String identifikator) {
		return new Person()
				.withNavn(navn)
				.withPersonidentifikator(identifikator);
	}

	private Aktoer createMottakerAktoerId() {
		return new AktoerId()
				.withNavn(MOTTAKER_ID_NAVN)
				.withAktoerId(MOTTAKER_ID);
	}

	private Aktoer createAktoerOrganisasjon() {
		return new Organisasjon()
				.withNavn(ORGANISASJON_NAVN)
				.withOrgnummer(ORGNUMMER);
	}

	private Aktoer createAktoerSamhandlerHpr() {
		return new Samhandler()
				.withNavn(SAMHANDLER_NAVN)
				.withSamhandleridentifikator(SAMHANDLER_IDENTIFIKATOR)
				.withSamhandlerkategori(SAMHANDLER_KATEGORI_HPR);
	}

}