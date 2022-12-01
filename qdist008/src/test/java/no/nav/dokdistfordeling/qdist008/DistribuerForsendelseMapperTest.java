package no.nav.dokdistfordeling.qdist008;

import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.domain.DistribuerForsendelseTo;
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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistribuerForsendelseMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String DISTRIBUSJONKANAL_PRINT = "PRINT";
	private static final String DISTRIBUSJONKANAL_SDP = "SDP";
	private static final String INGEN_DISTRIBUSJON = DistribusjonsKanalCode.INGEN_DISTRIBUSJON.name();
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
	private static final String SAMHANDLER_KATEGORI_UTL_ORG = "UTL_ORG";
	private static final String SAMHANDLER_KATEGORI_UKJENT = "UKJENT";
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

	private DistribuerForsendelseMapper distribuerForsendelseMapper = new DistribuerForsendelseMapper();

	@Test
	public void shouldMap() {
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(createDistribuerForsendelse());
		assertResponse(distribuerForsendelseTo);
	}

	@Test
	public void shouldMapWithKanalSDP() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setDistribusjonKanal(DISTRIBUSJONKANAL_SDP);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapWithKanalIngenDistribusjon() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setDistribusjonKanal(INGEN_DISTRIBUSJON);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withAdresse(createUtenlandskPostadresse());
		distribuerForsendelse.getDistribusjonbestilling().withDistribusjonKanal(DISTRIBUSJONKANAL_PRINT);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertUtenlandskPostadresseTo(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse());
	}

	@Test
	public void shouldMapAktoerId() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createMottakerAktoerId());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), MOTTAKER_ID);
		assertEquals(mottakerTo.getNavn(), MOTTAKER_ID_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.PERSON);
		assertTrue(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOrganisasjon() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createAktoerOrganisasjon());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getBruker());
		final DistribuerForsendelseTo.AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), ORGNUMMER);
		assertEquals(mottakerTo.getNavn(), ORGANISASJON_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.ORGANISASJON);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapSamhandlerHpr() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createAktoerSamhandlerHpr());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.SAMHANDLER_HPR);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapSamhandlerUtlOrg() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createAktoerSamhandlerOrgUtl());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.SAMHANDLER_UTL_ORG);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapSamhandlerUkjent() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createAktoerSamhandlerOrgUkjent());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.AktoerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getAktoerType(), AktoerTypeCode.SAMHANDLER_UKJENT);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOkWhenWithoutAkivinformasjon() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withArkivInformasjon(null);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldMapOkWhenWithoutBatchId() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setBatchId(null);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getBatchId());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	void shouldMapBlankBatchIdToNull(String batchId) {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setBatchId(batchId);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getBatchId());
	}

	@Test
	public void shouldMapOkWhenWithoutForsendelseTittel() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setForsendelseTittel(null);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getForsendelseTittel());
	}

	@Test
	public void shouldFailUgyldigSamhandlerKategori() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling()
				.withMottaker(((Samhandler) createAktoerSamhandlerHpr()).withSamhandlerkategori("NO_SUCH_KATEGORI"));

		assertThrows(AbstractDokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw AbstractDokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldFailUgyldigArkivsystem() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon().setArkivSystem("NO_SUCH_ARKIVSYSTEM");

		assertThrows(AbstractDokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw AbstractDokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldFailUgyldigTilknytning() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getDokumenter().get(0).setTilknyttetSom("NO_SUCH_TILKNYTNING");

		assertThrows(AbstractDokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw AbstractDokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldMapNorskAdresseWithEmptyString() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		NorskPostadresse adresse = createNorskPostadresse();
		adresse.setAdresselinje1("    ");
		adresse.setAdresselinje2("           ");
		adresse.setAdresselinje3("          " + adresse.getAdresselinje3());
		distribuerForsendelse.getDistribusjonbestilling().withAdresse(adresse);
		distribuerForsendelse.getDistribusjonbestilling().withDistribusjonKanal(DISTRIBUSJONKANAL_PRINT);
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje1());
		assertNull(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje2());
		assertEquals(distribuerForsendelseTo.getDistribusjonbestilling().getAdresse().getAdresselinje3(), ADRESSELINJE_3);
	}

	private void assertResponse(DistribuerForsendelseTo distribuerForsendelseTo) {
		assertDistribuerForsendelseTo(distribuerForsendelseTo);

		//assert DistribusjonbestillingTo
		final DistribuerForsendelseTo.DistribusjonbestillingTo distBestilling = distribuerForsendelseTo.getDistribusjonbestilling();
		assertEquals(distBestilling.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(distBestilling.getBatchId(), BATCH_ID);
		assertEquals(distBestilling.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(distBestilling.getTema(), TEMA);
		assertEquals(distBestilling.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(distBestilling.getDokumentProdApp(), DOKUMENT_PROD_APP);

		//assert Arkivinformasjon
		assertNotNull(distBestilling.getArkivInformasjon());
		assertEquals(distBestilling.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(distBestilling.getArkivInformasjon().getArkivSystem().name(), ARKIV_SYSTEM);

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
				.extracting(DistribuerForsendelseTo.DokumentInformasjonTo::getDokumenttypeId,
						DistribuerForsendelseTo.DokumentInformasjonTo::getDokumentObjektReferanse,
						DistribuerForsendelseTo.DokumentInformasjonTo::getTilknyttetSom,
						DistribuerForsendelseTo.DokumentInformasjonTo::getArkivDokumentInfoId,
						DistribuerForsendelseTo.DokumentInformasjonTo::getRekkefolge)
				.hasSize(2)
				.containsExactlyInAnyOrder(tuple(DOKUMENTTYPE_ID_1, OBJEKT_REFERANSE_1, TilknyttetSomCode.valueOf(TILKNYTTET_SOM_HOVEDDOK), ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1),
						tuple(DOKUMENTTYPE_ID_2, OBJEKT_REFERANSE_2, TilknyttetSomCode.valueOf(TILKNYTTET_SOM_VEDLEGG), ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2));
	}

	private void assertDistribuerForsendelseTo(DistribuerForsendelseTo distribuerForsendelseTo) {
		assertNotNull(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling());
	}

	private void assertNorskPostadresseTo(DistribuerForsendelseTo.AdresseTo adresse) {
		assertTrue(adresse instanceof DistribuerForsendelseTo.NorskPostadresseTo);
		final DistribuerForsendelseTo.NorskPostadresseTo postadresse = (DistribuerForsendelseTo.NorskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLand(), LAND);
	}

	private void assertUtenlandskPostadresseTo(DistribuerForsendelseTo.AdresseTo adresse) {
		assertTrue(adresse instanceof DistribuerForsendelseTo.UtenlandskPostadresseTo);
		final DistribuerForsendelseTo.UtenlandskPostadresseTo postadresse = (DistribuerForsendelseTo.UtenlandskPostadresseTo) adresse;
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getLand(), LAND);
	}

	private DistribuerForsendelse createDistribuerForsendelse() {
		return new DistribuerForsendelse()
				.withDistribusjonbestilling(new Distribusjonbestilling()
						.withBestillingsId(BESTILLINGS_ID)
						.withBatchId(BATCH_ID)
						.withDistribusjonKanal(DISTRIBUSJONKANAL_PRINT)
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
										.withDokumentObjektReferanse(OBJEKT_REFERANSE_1)
										.withTilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
										.withArkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
										.withRekkefolge(REKKEFOLGE_1),
								new DokumentInformasjon()
										.withDokumenttypeId(DOKUMENTTYPE_ID_2)
										.withDokumentObjektReferanse(OBJEKT_REFERANSE_2)
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

	private Aktoer createAktoerSamhandlerOrgUtl() {
		return new Samhandler()
				.withNavn(SAMHANDLER_NAVN)
				.withSamhandleridentifikator(SAMHANDLER_IDENTIFIKATOR)
				.withSamhandlerkategori(SAMHANDLER_KATEGORI_UTL_ORG);
	}

	private Aktoer createAktoerSamhandlerOrgUkjent() {
		return new Samhandler()
				.withNavn(SAMHANDLER_NAVN)
				.withSamhandleridentifikator(SAMHANDLER_IDENTIFIKATOR)
				.withSamhandlerkategori(SAMHANDLER_KATEGORI_UKJENT);
	}
}
