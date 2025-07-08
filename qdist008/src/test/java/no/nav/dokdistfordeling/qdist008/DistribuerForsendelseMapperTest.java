package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.AktoerTo;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo.DokumentInformasjonTo;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.UtenlandskPostadresse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_ARKIVMELDING;
import static no.nav.dokdistfordeling.kodeverk.ForsendelseMetadataType.DPO_AVTALEMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistribuerForsendelseMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String DISTRIBUSJONKANAL_PRINT = "PRINT";
	private static final String DISTRIBUSJONKANAL_SDP = "SDP";
	private static final String DISTRIBUSJONSKANAL_INGEN_DISTRIBUSJON = "INGEN_DISTRIBUSJON";
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
	private static final String OBJEKT_REFERANSE_1 = "objektReferanse1";
	private static final String OBJEKT_REFERANSE_2 = "objektReferanse2";
	private static final String TILKNYTTET_SOM_HOVEDDOK = "HOVEDDOKUMENT";
	private static final String TILKNYTTET_SOM_VEDLEGG = "VEDLEGG";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";
	private static final int REKKEFOLGE_1 = 1;
	private static final int REKKEFOLGE_2 = 2;

	private final DistribuerForsendelseMapper distribuerForsendelseMapper = new DistribuerForsendelseMapper();

	@Test
	public void shouldMap() {
		var distribuerForsendelse = createDistribuerForsendelse();

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertResponse(distribuerForsendelseTo);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	@NullSource
	void shouldMapBlankOrNullBatchIdToNull(String batchId) {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setBatchId(batchId);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getBatchId());
	}

	@ParameterizedTest
	@ValueSource(strings = {DISTRIBUSJONKANAL_SDP, DISTRIBUSJONSKANAL_INGEN_DISTRIBUSJON})
	public void shouldMapWhenKanalIsSDPOrIngenDistribusjon(String distribusjonskanal) {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setDistribusjonKanal(distribusjonskanal);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNorskPostadresseTo(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapOkWhenForsendelseTittelIsNull() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setForsendelseTittel(null);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getForsendelseTittel());
	}

	@Test
	public void shouldMapOkWhenAkivinformasjonIsNull() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setArkivInformasjon(null);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldFailUgyldigArkivsystem() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon().setArkivSystem("NO_SUCH_ARKIVSYSTEM");

		assertThrows(AbstractDokdistfordelingFunctionalException.class, () -> distribuerForsendelseMapper.map(distribuerForsendelse));
	}

	@Test
	public void shouldMapAktoerId() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setMottaker(createMottakerAktoerId());

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(MOTTAKER_ID, mottakerTo.getIdentifikator());
		assertEquals(MOTTAKER_ID_NAVN, mottakerTo.getNavn());
		assertEquals(AktoerTypeCode.PERSON, mottakerTo.getAktoerType());
		assertTrue(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOrganisasjon() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setMottaker(createAktoerOrganisasjon());

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getBruker());
		final AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(ORGNUMMER, mottakerTo.getIdentifikator());
		assertEquals(ORGANISASJON_NAVN, mottakerTo.getNavn());
		assertEquals(AktoerTypeCode.ORGANISASJON, mottakerTo.getAktoerType());
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@ParameterizedTest
	@MethodSource
	public void shouldMapSamhandler(String samhandlerkategori, AktoerTypeCode aktoerTypeCode) {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setMottaker(createAktoerSamhandler(samhandlerkategori));

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(SAMHANDLER_IDENTIFIKATOR, mottakerTo.getIdentifikator());
		assertEquals(SAMHANDLER_NAVN, mottakerTo.getNavn());
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

	@ParameterizedTest
	@MethodSource
	public void shouldMapForsendelseMetadataAndType(String forsendelseMetadata, String forsendelseMetadataType, ForsendelseMetadataType forventetForsendelseMetadataType) {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setForsendelseMetadata(forsendelseMetadata);
		distribuerForsendelse.getDistribusjonbestilling().setForsendelseMetadataType(forsendelseMetadataType);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertEquals(forsendelseMetadata, distribuerForsendelseTo.getDistribusjonbestilling().getForsendelseMetadata());
		assertEquals(forventetForsendelseMetadataType, distribuerForsendelseTo.getDistribusjonbestilling().getForsendelseMetadataType());
	}

	private static Stream<Arguments> shouldMapForsendelseMetadataAndType() {
		return Stream.of(
				Arguments.of("forsendelseMetadata", "DPO_ARKIVMELDING", DPO_ARKIVMELDING),
				Arguments.of("forsendelseMetadata", "DPO_AVTALEMELDING", DPO_AVTALEMELDING),
				Arguments.of("forsendelseMetadata", "dpo_avtalemelding", DPO_AVTALEMELDING),
				Arguments.of(null, null, null)
		);
	}

	@Test
	public void shouldFailUgyldigSamhandlerKategori() {
		var distribuerForsendelse = createDistribuerForsendelse();
		Samhandler aktoerSamhandlerHpr = (Samhandler) createAktoerSamhandler(SAMHANDLER_KATEGORI_HPR);
		aktoerSamhandlerHpr.setSamhandlerkategori("NO_SUCH_KATEGORI");
		distribuerForsendelse.getDistribusjonbestilling().setMottaker(aktoerSamhandlerHpr);

		assertThrows(AbstractDokdistfordelingFunctionalException.class, () -> distribuerForsendelseMapper.map(distribuerForsendelse));
	}

	@Test
	public void shouldMapNorskAdresseWithEmptyString() {
		var distribuerForsendelse = createDistribuerForsendelse();
		NorskPostadresse adresse = createNorskPostadresse();
		adresse.setAdresselinje1("    ");
		adresse.setAdresselinje2("           ");
		adresse.setAdresselinje3("          " + adresse.getAdresselinje3());
		distribuerForsendelse.getDistribusjonbestilling().setAdresse(adresse);
		distribuerForsendelse.getDistribusjonbestilling().setDistribusjonKanal(DISTRIBUSJONKANAL_PRINT);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje1());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje2());
		assertEquals(ADRESSELINJE_3, distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje3());
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setAdresse(createUtenlandskPostadresse());
		distribuerForsendelse.getDistribusjonbestilling().setDistribusjonKanal(DISTRIBUSJONKANAL_PRINT);

		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
		assertUtenlandskPostadresseTo(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldFailUgyldigTilknytning() {
		var distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getDokumenter().get(0).setTilknyttetSom("NO_SUCH_TILKNYTNING");

		assertThrows(AbstractDokdistfordelingFunctionalException.class, () -> distribuerForsendelseMapper.map(distribuerForsendelse));
	}

	private void assertResponse(DistribuerForsendelseTo distribuerForsendelseTo) {
		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());

		//assert DistribusjonbestillingTo
		final DistribuerForsendelseTo.DistribusjonbestillingTo distBestilling = distribuerForsendelseTo.getDistribusjonbestilling();
		assertEquals(BESTILLINGS_ID, distBestilling.getBestillingsId());
		assertEquals(BATCH_ID, distBestilling.getBatchId());
		assertEquals(BESTILLENDE_FAGSYSTEM, distBestilling.getBestillendeFagsystem());
		assertEquals(TEMA, distBestilling.getTema());
		assertEquals(FORSENDELSE_TITTEL, distBestilling.getForsendelseTittel());
		assertEquals(DOKUMENT_PROD_APP, distBestilling.getDokumentProdApp());

		//assert Arkivinformasjon
		assertNotNull(distBestilling.getArkivInformasjon());
		assertEquals(ARKIV_ID, distBestilling.getArkivInformasjon().getArkivId());
		assertEquals(ARKIV_SYSTEM, distBestilling.getArkivInformasjon().getArkivSystem().name());

		//assert mottaker Person
		assertNotNull(distBestilling.getMottaker());
		assertEquals(PERSON_IDENTIFIKATOR_MOTTAKER, distBestilling.getMottaker().getIdentifikator());
		assertEquals(PERSON_NAVN_MOTTAKER, distBestilling.getMottaker().getNavn());
		assertEquals(AktoerTypeCode.PERSON, distBestilling.getMottaker().getAktoerType());
		assertFalse(distBestilling.getMottaker().isIdentifikatorAktoerId());

		//assert bruker Person
		assertNotNull(distBestilling.getBruker());
		assertEquals(PERSON_IDENTIFIKATOR_BRUKER, distBestilling.getBruker().getIdentifikator());
		assertEquals(PERSON_NAVN_BRUKER, distBestilling.getBruker().getNavn());
		assertEquals(AktoerTypeCode.PERSON, distBestilling.getBruker().getAktoerType());
		assertFalse(distBestilling.getBruker().isIdentifikatorAktoerId());


		//assert norsk postadresse
		assertNorskPostadresseTo(distBestilling.getAdresse());

		//assert dokumenter
		assertThat(distBestilling.getDokumenter())
				.extracting(DokumentInformasjonTo::getDokumenttypeId,
						DokumentInformasjonTo::getDokumentObjektReferanse,
						DokumentInformasjonTo::getTilknyttetSom,
						DokumentInformasjonTo::getArkivDokumentInfoId,
						DokumentInformasjonTo::getRekkefolge)
				.hasSize(2)
				.containsExactlyInAnyOrder(
						tuple(DOKUMENTTYPE_ID_1, OBJEKT_REFERANSE_1, TilknyttetSomCode.valueOf(TILKNYTTET_SOM_HOVEDDOK), ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1),
						tuple(DOKUMENTTYPE_ID_2, OBJEKT_REFERANSE_2, TilknyttetSomCode.valueOf(TILKNYTTET_SOM_VEDLEGG), ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2)
				);
	}

	private void assertNorskPostadresseTo(DistribuerForsendelseTo.AdresseTo adresse) {
		assertInstanceOf(DistribuerForsendelseTo.NorskPostadresseTo.class, adresse);
		final DistribuerForsendelseTo.NorskPostadresseTo postadresse = (DistribuerForsendelseTo.NorskPostadresseTo) adresse;
		assertEquals(ADRESSELINJE_1, postadresse.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresse.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresse.getAdresselinje3());
		assertEquals(POSTNUMMER, postadresse.getPostnummer());
		assertEquals(POSTSTED, postadresse.getPoststed());
		assertEquals(LAND, postadresse.getLand());
	}

	private void assertUtenlandskPostadresseTo(DistribuerForsendelseTo.AdresseTo adresse) {
		assertInstanceOf(DistribuerForsendelseTo.UtenlandskPostadresseTo.class, adresse);
		final DistribuerForsendelseTo.UtenlandskPostadresseTo postadresse = (DistribuerForsendelseTo.UtenlandskPostadresseTo) adresse;
		assertEquals(ADRESSELINJE_1, postadresse.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresse.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresse.getAdresselinje3());
		assertEquals(LAND, postadresse.getLand());
	}

	private DistribuerForsendelse createDistribuerForsendelse() {
		Distribusjonbestilling distribusjonbestilling = new Distribusjonbestilling();
		distribusjonbestilling.setBestillingsId(BESTILLINGS_ID);
		distribusjonbestilling.setBatchId(BATCH_ID);
		distribusjonbestilling.setDistribusjonKanal(DISTRIBUSJONKANAL_PRINT);
		distribusjonbestilling.setBestillendeFagsystem(BESTILLENDE_FAGSYSTEM);
		distribusjonbestilling.setTema(TEMA);
		distribusjonbestilling.setForsendelseTittel(FORSENDELSE_TITTEL);
		distribusjonbestilling.setMottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER));
		distribusjonbestilling.setBruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER));
		distribusjonbestilling.setAdresse(createNorskPostadresse());
		distribusjonbestilling.setDokumentProdApp(DOKUMENT_PROD_APP);

		ArkivInformasjon arkivInformasjon = new ArkivInformasjon();
		arkivInformasjon.setArkivId(ARKIV_ID);
		arkivInformasjon.setArkivSystem(ARKIV_SYSTEM);
		distribusjonbestilling.setArkivInformasjon(arkivInformasjon);

		DokumentInformasjon dokumentInformasjon1 = new DokumentInformasjon();
		dokumentInformasjon1.setDokumenttypeId(DOKUMENTTYPE_ID_1);
		dokumentInformasjon1.setDokumentObjektReferanse(OBJEKT_REFERANSE_1);
		dokumentInformasjon1.setTilknyttetSom(TILKNYTTET_SOM_HOVEDDOK);
		dokumentInformasjon1.setArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1);
		dokumentInformasjon1.setRekkefolge(REKKEFOLGE_1);

		DokumentInformasjon dokumentInformasjon2 = new DokumentInformasjon();
		dokumentInformasjon2.setDokumenttypeId(DOKUMENTTYPE_ID_2);
		dokumentInformasjon2.setDokumentObjektReferanse(OBJEKT_REFERANSE_2);
		dokumentInformasjon2.setTilknyttetSom(TILKNYTTET_SOM_VEDLEGG);
		dokumentInformasjon2.setArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2);
		dokumentInformasjon2.setRekkefolge(REKKEFOLGE_2);

		distribusjonbestilling.setDokumenter(Arrays.asList(dokumentInformasjon1, dokumentInformasjon2));

		DistribuerForsendelse distribuerForsendelse = new DistribuerForsendelse();
		distribuerForsendelse.setDistribusjonbestilling(distribusjonbestilling);
		return distribuerForsendelse;
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