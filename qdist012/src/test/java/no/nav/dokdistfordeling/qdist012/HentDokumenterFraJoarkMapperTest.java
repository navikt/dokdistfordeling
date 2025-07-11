package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AdresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.UtenlandskPostadresseTo;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static no.nav.dokdistfordeling.constants.Constants.DITT_NAV;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.ORGANISASJON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.PERSON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.DITTNAV;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.UMIDDELBART;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_ARKIVMELDING;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_AVTALEMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setDistribusjonstype(VEDTAK.name());
		hentDokumenterFraJoark.getDistribusjonbestilling().setDistribusjonstidspunkt(UMIDDELBART.name());

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertResponse(hentDokumenterFraJoarkTo);
	}

	@Test
	public void shouldMapOkWhenBatchIdIsNull() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setBatchId(null);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getBatchId());
	}

	@Test
	public void shouldMapDistribusjonskanalDittNAV() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoarkDittNav();

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		DistribusjonbestillingTo distribusjonbestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		assertEquals(distribusjonbestilling.getDistribusjonKanal(), DITTNAV.name());
		assertThat(distribusjonbestilling.getAdresse()).isNotNull();
	}

	@Test
	public void shouldMapOkWhenForsendelseTittelIsNull() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setForsendelseTittel(null);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getForsendelseTittel());
	}

	@Test
	public void shouldMapOkWhenForsendelseMetadataIsNull() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setForsendelseMetadata(null);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getForsendelseMetadata());
	}

	@ParameterizedTest
	@MethodSource
	void shouldMapForsendelseMetadataType(String forsendelseMetadataType, ForsendelseMetadataType forventetForsendelseMetadataType) {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setForsendelseMetadataType(forsendelseMetadataType);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertThat(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getForsendelseMetadataType()).isEqualTo(forventetForsendelseMetadataType);
	}

	private static Stream<Arguments> shouldMapForsendelseMetadataType() {
		return Stream.of(
				Arguments.of("DPO_ARKIVMELDING", DPO_ARKIVMELDING),
				Arguments.of("DPO_AVTALEMELDING", DPO_AVTALEMELDING),
				Arguments.of("dpo_avtalemelding", DPO_AVTALEMELDING),
				Arguments.of(null, null)
		);
	}

	@Test
	public void shouldNotMapDistribusjonstypeAndDistribusjonstidspunktIfNullOrInvalidValue() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setDistribusjonstype(null);
		hentDokumenterFraJoark.getDistribusjonbestilling().setDistribusjonstidspunkt(UMIDDELBART.name().toLowerCase());

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertEquals(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getDistribusjonstidspunkt(), UMIDDELBART);
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getDistribusjonstype());
	}

	@Test
	public void shouldMapOkWhenArkivinformasjonIsNull() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setArkivInformasjon(null);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldMapAktoerId() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setMottaker(createMottakerAktoerId());

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), MOTTAKER_ID);
		assertEquals(mottakerTo.getNavn(), MOTTAKER_ID_NAVN);
		assertEquals(mottakerTo.getAktoerType(), PERSON);
		assertTrue(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOrganisasjon() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setMottaker(createAktoerOrganisasjon());

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getBruker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), ORGNUMMER);
		assertEquals(mottakerTo.getNavn(), ORGANISASJON_NAVN);
		assertEquals(mottakerTo.getAktoerType(), ORGANISASJON);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@ParameterizedTest
	@MethodSource
	public void shouldMapSamhandler(String samhandlerkategori, AktoerTypeCode aktoerTypeCode) {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setMottaker(createAktoerSamhandler(samhandlerkategori));

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker());
		final HentDokumenterFraJoarkTo.AktoerTo mottakerTo = hentDokumenterFraJoarkTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getAktoerType(), aktoerTypeCode);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	private static Stream<Arguments> shouldMapSamhandler() {
		return Stream.of(
				Arguments.of("HPR", SAMHANDLER_HPR),
				Arguments.of("UKJENT", SAMHANDLER_UKJENT),
				Arguments.of("UTL_ORG", SAMHANDLER_UTL_ORG)
		);
	}

	@Test
	public void shouldMapToDittNAVWhenAdresseIsNull() {
		var hentDokumenterFraJoarkDittNav = createHentDokumenterFraJoarkDittNav();
		hentDokumenterFraJoarkDittNav.getDistribusjonbestilling().setAdresse(null);

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoarkDittNav);

		DistribusjonbestillingTo distribusjonbestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		assertEquals(distribusjonbestilling.getDistribusjonKanal(), DITTNAV.name());
		assertThat(distribusjonbestilling.getAdresse()).isNull();
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		var hentDokumenterFraJoark = createHentDokumenterFraJoark();
		hentDokumenterFraJoark.getDistribusjonbestilling().setAdresse(createUtenlandskPostadresse());

		HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo = hentDokumenterFraJoarkMapper.map(hentDokumenterFraJoark);

		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());
		assertUtenlandskPostadresseTo(hentDokumenterFraJoarkTo.getDistribusjonbestilling().getAdresse());
	}

	private void assertResponse(HentDokumenterFraJoarkTo hentDokumenterFraJoarkTo) {
		assertNotNull(hentDokumenterFraJoarkTo);
		assertNotNull(hentDokumenterFraJoarkTo.getDistribusjonbestilling());

		//assert HentDokumenterFraJoarkTo
		final DistribusjonbestillingTo distBestilling = hentDokumenterFraJoarkTo.getDistribusjonbestilling();
		assertEquals(distBestilling.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(distBestilling.getBatchId(), BATCH_ID);
		assertEquals(distBestilling.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(distBestilling.getTema(), TEMA);
		assertEquals(distBestilling.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(distBestilling.getDokumentProdApp(), DOKUMENT_PROD_APP);
		assertEquals(distBestilling.getDistribusjonstype(), VEDTAK);
		assertEquals(distBestilling.getDistribusjonstidspunkt(), UMIDDELBART);

		//assert Arkivinformasjon
		assertNotNull(distBestilling.getArkivInformasjon());
		assertEquals(distBestilling.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(distBestilling.getArkivInformasjon().getArkivSystem(), ARKIV_SYSTEM);

		//assert mottaker Person
		assertNotNull(distBestilling.getMottaker());
		assertEquals(distBestilling.getMottaker().getIdentifikator(), PERSON_IDENTIFIKATOR_MOTTAKER);
		assertEquals(distBestilling.getMottaker().getNavn(), PERSON_NAVN_MOTTAKER);
		assertEquals(distBestilling.getMottaker().getAktoerType(), PERSON);
		assertFalse(distBestilling.getMottaker().isIdentifikatorAktoerId());

		//assert bruker Person
		assertNotNull(distBestilling.getBruker());
		assertEquals(distBestilling.getBruker().getIdentifikator(), PERSON_IDENTIFIKATOR_BRUKER);
		assertEquals(distBestilling.getBruker().getNavn(), PERSON_NAVN_BRUKER);
		assertEquals(distBestilling.getBruker().getAktoerType(), PERSON);
		assertFalse(distBestilling.getBruker().isIdentifikatorAktoerId());

		//assert norsk postadresse
		assertNorskPostadresseTo(distBestilling.getAdresse());

		//assert dokumenter
		assertThat(distBestilling.getDokumenter())
				.extracting(HentDokumenterFraJoarkTo.DokumentInformasjonTo::getDokumenttypeId,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getDokumentObjektReferanse,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getTilknyttetSom,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getArkivDokumentInfoId,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getRekkefolge,
						HentDokumenterFraJoarkTo.DokumentInformasjonTo::getVariantFormat)
				.hasSize(2)
				.containsExactlyInAnyOrder(
						tuple(DOKUMENTTYPE_ID_1, null, TILKNYTTET_SOM_HOVEDDOK, ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1, VARIANTFORMAT_1),
						tuple(DOKUMENTTYPE_ID_2, null, TILKNYTTET_SOM_VEDLEGG, ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2, VARIANTFORMAT_2)
				);
	}

	private void assertNorskPostadresseTo(AdresseTo adresse) {
		assertTrue(adresse instanceof NorskPostadresseTo);
		final NorskPostadresseTo postadresse = (NorskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLand(), LAND);
	}

	private void assertUtenlandskPostadresseTo(AdresseTo adresse) {
		assertTrue(adresse instanceof UtenlandskPostadresseTo);
		final UtenlandskPostadresseTo postadresse = (UtenlandskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getLand(), LAND);
	}

	private HentDokumenterFraJoark createHentDokumenterFraJoark() {
		return createHentDokumenterFraJoark(PRINT.name());
	}

	private HentDokumenterFraJoark createHentDokumenterFraJoarkDittNav() {
		return createHentDokumenterFraJoark(DITT_NAV);
	}

	private HentDokumenterFraJoark createHentDokumenterFraJoark(String distribusjonKanal) {
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(BESTILLINGS_ID);
		distribusjonbestilling.setBatchId(BATCH_ID);
		distribusjonbestilling.setDistribusjonKanal(distribusjonKanal);
		distribusjonbestilling.setBestillendeFagsystem(BESTILLENDE_FAGSYSTEM);
		distribusjonbestilling.setTema(TEMA);
		distribusjonbestilling.setForsendelseTittel(FORSENDELSE_TITTEL);

		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivId(ARKIV_ID);
		arkivInformasjon.setArkivSystem(ARKIV_SYSTEM);
		distribusjonbestilling.setArkivInformasjon(arkivInformasjon);

		distribusjonbestilling.setMottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER));
		distribusjonbestilling.setBruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER));
		distribusjonbestilling.setAdresse(createNorskPostadresse());
		distribusjonbestilling.setDokumentProdApp(DOKUMENT_PROD_APP);

		DokumentInformasjon dokumentInformasjon1 = new DokumentInformasjon();
		dokumentInformasjon1.setDokumenttypeId(DOKUMENTTYPE_ID_1);
		dokumentInformasjon1.setVariantFormat(VARIANTFORMAT_1);
		dokumentInformasjon1.setTilknyttetSom(TILKNYTTET_SOM_HOVEDDOK);
		dokumentInformasjon1.setArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1);
		dokumentInformasjon1.setRekkefolge(REKKEFOLGE_1);

		DokumentInformasjon dokumentInformasjon2 = new DokumentInformasjon();
		dokumentInformasjon2.setDokumenttypeId(DOKUMENTTYPE_ID_2);
		dokumentInformasjon2.setVariantFormat(VARIANTFORMAT_2);
		dokumentInformasjon2.setTilknyttetSom(TILKNYTTET_SOM_VEDLEGG);
		dokumentInformasjon2.setArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2);
		dokumentInformasjon2.setRekkefolge(REKKEFOLGE_2);


		distribusjonbestilling.setDokumenter(Arrays.asList(dokumentInformasjon1, dokumentInformasjon2));

		HentDokumenterFraJoark hentDokumenterFraJoark = new HentDokumenterFraJoark();
		hentDokumenterFraJoark.setDistribusjonbestilling(distribusjonbestilling);

		return hentDokumenterFraJoark;
	}

	private NorskPostadresse createNorskPostadresse() {
		NorskPostadresse norskPostadresse = new NorskPostadresse();
		norskPostadresse.setAdresselinje1(ADRESSELINJE_1);
		norskPostadresse.setAdresselinje2(ADRESSELINJE_2);
		norskPostadresse.setAdresselinje3(ADRESSELINJE_3);
		norskPostadresse.setPostnummer(POSTNUMMER);
		norskPostadresse.setPoststed(POSTSTED);
		norskPostadresse.setLand(LAND);
		return norskPostadresse;
	}

	private UtenlandskPostadresse createUtenlandskPostadresse() {
		UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
		utenlandskPostadresse.setAdresselinje1(ADRESSELINJE_1);
		utenlandskPostadresse.setAdresselinje2(ADRESSELINJE_2);
		utenlandskPostadresse.setAdresselinje3(ADRESSELINJE_3);
		utenlandskPostadresse.setLand(LAND);
		return utenlandskPostadresse;
	}

	private Aktoer createAktoerPerson(String navn, String identifikator) {
		Person person = new Person();
		person.setNavn(navn);
		person.setPersonidentifikator(identifikator);
		return person;
	}

	private Aktoer createMottakerAktoerId() {
		AktoerId aktoerId = new AktoerId();
		aktoerId.setNavn(MOTTAKER_ID_NAVN);
		aktoerId.setAktoerId(MOTTAKER_ID);
		return aktoerId;
	}

	private Aktoer createAktoerOrganisasjon() {
		Organisasjon organisasjon = new Organisasjon();
		organisasjon.setNavn(ORGANISASJON_NAVN);
		organisasjon.setOrgnummer(ORGNUMMER);
		return organisasjon;
	}

	private Aktoer createAktoerSamhandler(String samhandlerkategori) {
		Samhandler samhandler = new Samhandler();
		samhandler.setNavn(SAMHANDLER_NAVN);
		samhandler.setSamhandleridentifikator(SAMHANDLER_IDENTIFIKATOR);
		samhandler.setSamhandlerkategori(samhandlerkategori);
		return samhandler;
	}

}