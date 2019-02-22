package no.nav.dokdistfordeling.qdist008;

import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.nav.dokdistfordeling.exception.DokdistfordelingFunctionalException;
import no.nav.dokdistfordeling.kodeverk.MottakerTypeCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.Aktoer;
import no.nav.meldinger.virksomhet.dokdistfordeling.AktoerId;
import no.nav.meldinger.virksomhet.dokdistfordeling.ArkivInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.DistribuerForsendelse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Distribusjonbestilling;
import no.nav.meldinger.virksomhet.dokdistfordeling.DokumentInformasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.NorskPostadresse;
import no.nav.meldinger.virksomhet.dokdistfordeling.Organisasjon;
import no.nav.meldinger.virksomhet.dokdistfordeling.Person;
import no.nav.meldinger.virksomhet.dokdistfordeling.Samhandler;
import no.nav.meldinger.virksomhet.dokdistfordeling.UtenlandskPostadresse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
class DistribuerForsendelseMapperTest {

	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String TEMA = "FS22";
	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String ARKIV_SYSTEM = "JOARK";
	private static final String ARKIV_ID = "arkivId";
	private static final String PERSON_NAVN = "personNavn";
	private static final String AKTOER_ID_NAVN = "aktoerIdNavn";
	private static final String ORGANISASJON_NAVN = "organisasjonNavn";
	private static final String SAMHANDLER_NAVN = "samhandlerNavn";
	private static final String PERSON_IDENTIFIKATOR = "personId";
	private static final String AKTOER_ID = "aktoerId";
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

	private DistribuerForsendelseMapper distribuerForsendelseMapper = new DistribuerForsendelseMapper();


	@Test
	public void shouldMap() {
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(createDistribuerForsendelse());
		assertResponse(distribuerForsendelseTo);
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withAdresse(createUtenlandskPostadresse());
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
		final DistribuerForsendelseTo.MottakerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), AKTOER_ID);
		assertEquals(mottakerTo.getNavn(), AKTOER_ID_NAVN);
		assertEquals(mottakerTo.getMottakerType(), MottakerTypeCode.PERSON);
		assertTrue(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapOrganisasjon() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createMottakerOrganisasjon());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.MottakerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), ORGNUMMER);
		assertEquals(mottakerTo.getNavn(), ORGANISASJON_NAVN);
		assertEquals(mottakerTo.getMottakerType(), MottakerTypeCode.ORGANISASJON);
		assertFalse(mottakerTo.isIdentifikatorAktoerId());
	}

	@Test
	public void shouldMapSamhandlerHpr() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().withMottaker(createMottakerSamhandlerHpr());
		DistribuerForsendelseTo distribuerForsendelseTo = distribuerForsendelseMapper.map(distribuerForsendelse);

		assertDistribuerForsendelseTo(distribuerForsendelseTo);
		assertNotNull(distribuerForsendelseTo.getDistribusjonbestilling().getMottaker());
		final DistribuerForsendelseTo.MottakerTo mottakerTo = distribuerForsendelseTo.getDistribusjonbestilling().getMottaker();
		assertEquals(mottakerTo.getIdentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(mottakerTo.getNavn(), SAMHANDLER_NAVN);
		assertEquals(mottakerTo.getMottakerType(), MottakerTypeCode.SAMHANDLER_HPR);
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
				.withMottaker(((Samhandler) createMottakerSamhandlerHpr()).withSamhandlerkategori("NO_SUCH_KATEGORI"));

		assertThrows(DokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw DokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldFailUgyldigArkivsystem() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon().setArkivSystem("NO_SUCH_ARKIVSYSTEM");

		assertThrows(DokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw DokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldFailUgyldigTema() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().setTema("NO_SUCH_TEMA");

		assertThrows(DokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw DokdistfordelingFunctionalException, but it didn't");
	}

	@Test
	public void shouldFailUgyldigTilknytning() {
		DistribuerForsendelse distribuerForsendelse = createDistribuerForsendelse();
		distribuerForsendelse.getDistribusjonbestilling().getDokumenter().get(0).setTilknyttetSom("NO_SUCH_TILKNYTNING");

		assertThrows(DokdistfordelingFunctionalException.class,
				() -> distribuerForsendelseMapper.map(distribuerForsendelse),
				"Expected distribuerForsendelseMapper.map() to throw DokdistfordelingFunctionalException, but it didn't");
	}


	private void assertResponse(DistribuerForsendelseTo distribuerForsendelseTo) {
		assertDistribuerForsendelseTo(distribuerForsendelseTo);

		//assert DistribusjonbestillingTo
		final DistribuerForsendelseTo.DistribusjonbestillingTo distBestilling = distribuerForsendelseTo.getDistribusjonbestilling();
		assertEquals(distBestilling.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(distBestilling.getBatchId(), BATCH_ID);
		assertEquals(distBestilling.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(distBestilling.getTema().name(), TEMA);
		assertEquals(distBestilling.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(distBestilling.getDokumentProdApp(), DOKUMENT_PROD_APP);

		//assert Arkivinformasjon
		assertNotNull(distBestilling.getArkivInformasjon());
		assertEquals(distBestilling.getArkivInformasjon().getArkivId(), ARKIV_ID);
		assertEquals(distBestilling.getArkivInformasjon().getArkivSystem().name(), ARKIV_SYSTEM);

		//assert mottaker Person
		assertNotNull(distBestilling.getMottaker());
		assertEquals(distBestilling.getMottaker().getIdentifikator(), PERSON_IDENTIFIKATOR);
		assertEquals(distBestilling.getMottaker().getNavn(), PERSON_NAVN);
		assertEquals(distBestilling.getMottaker().getMottakerType(), MottakerTypeCode.PERSON);
		assertFalse(distBestilling.getMottaker().isIdentifikatorAktoerId());

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
						.withBestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
						.withTema(TEMA)
						.withForsendelseTittel(FORSENDELSE_TITTEL)
						.withArkivInformasjon(new ArkivInformasjon()
								.withArkivId(ARKIV_ID)
								.withArkivSystem(ARKIV_SYSTEM))
						.withMottaker(createMottakerPerson())
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

	private Aktoer createMottakerPerson() {
		return new Person()
				.withNavn(PERSON_NAVN)
				.withPersonidentifikator(PERSON_IDENTIFIKATOR);
	}

	private Aktoer createMottakerAktoerId() {
		return new AktoerId()
				.withNavn(AKTOER_ID_NAVN)
				.withAktoerId(AKTOER_ID);
	}

	private Aktoer createMottakerOrganisasjon() {
		return new Organisasjon()
				.withNavn(ORGANISASJON_NAVN)
				.withOrgnummer(ORGNUMMER);
	}

	private Aktoer createMottakerSamhandlerHpr() {
		return new Samhandler()
				.withNavn(SAMHANDLER_NAVN)
				.withSamhandleridentifikator(SAMHANDLER_IDENTIFIKATOR)
				.withSamhandlerkategori(SAMHANDLER_KATEGORI_HPR);
	}
}
