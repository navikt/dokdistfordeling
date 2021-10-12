package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.meldinger.virksomhet.dokdistfordeling.qdist008.in.AktoerId;
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

import java.util.Arrays;

import static no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode.PRINT;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting.
 */

class Qdist008DistribuerForsendelseMapperTest {

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
	private static final String SAMHANDLER_KATEGORI_UTL_ORG = "UTL_ORG";
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

	private final Qdist008DistribuerForsendelseMapper qdist008DistribuerForsendelseMapper = new Qdist008DistribuerForsendelseMapper();

	@Test
	public void shouldMap() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(
				HentDokumenterFraJoarkTo.builder()
						.distribusjonbestilling(createDistribusjonbestillingToBuilder().distribusjonKanal(PRINT.name())
								.build())
						.build());

		assertResponse(distribuerForsendelse);
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(
				HentDokumenterFraJoarkTo.builder()
						.distribusjonbestilling(createDistribusjonbestillingToBuilder()
								.distribusjonKanal(PRINT.name())
								.adresse(createUtenlandskPostadresse())
								.build())
						.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertUtenlandskPostadresseTo(distribuerForsendelse.getDistribusjonbestilling());
	}

	@Test
	public void shouldMapAktoerId() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerAktoerId(MOTTAKER_ID_NAVN, MOTTAKER_ID))
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof AktoerId);

		final AktoerId mottaker = (AktoerId) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(mottaker.getAktoerId(), MOTTAKER_ID);
		assertEquals(mottaker.getNavn(), MOTTAKER_ID_NAVN);
	}

	@Test
	public void shouldMapOrganisasjon() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerOrganisasjon(ORGANISASJON_NAVN, ORGNUMMER))
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof Organisasjon);

		final Organisasjon organisasjon = (Organisasjon) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(organisasjon.getOrgnummer(), ORGNUMMER);
		assertEquals(organisasjon.getNavn(), ORGANISASJON_NAVN);
	}

	@Test
	public void shouldMapSamhandlerHpr() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerSamhandlerHpr(SAMHANDLER_NAVN, SAMHANDLER_IDENTIFIKATOR))
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof Samhandler);

		final Samhandler samhandler = (Samhandler) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(samhandler.getSamhandleridentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(samhandler.getNavn(), SAMHANDLER_NAVN);
		assertEquals(samhandler.getSamhandlerkategori(), SAMHANDLER_KATEGORI_HPR);
	}

	@Test
	public void shouldMapSamhandlerUtlOrg() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerSamhandlerUtlOrg(SAMHANDLER_NAVN, SAMHANDLER_IDENTIFIKATOR))
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof Samhandler);

		final Samhandler samhandler = (Samhandler) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(samhandler.getSamhandleridentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(samhandler.getNavn(), SAMHANDLER_NAVN);
		assertEquals(samhandler.getSamhandlerkategori(), SAMHANDLER_KATEGORI_UTL_ORG);
	}

	@Test
	public void shouldMapOkWhenWithoutAkivinformasjon() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.arkivInformasjon(null)
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldMapOkWhenWithoutBatchId() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.batchId(null)
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getBatchId());
	}

	@Test
	public void shouldMapOkWhenWithoutForsendelseTittel() {
		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.forsendelseTittel(null)
						.build())
				.build());

		assertDistribuerForsendelse(distribuerForsendelse);
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getForsendelseTittel());
	}

	private void assertResponse(DistribuerForsendelse distribuerForsendelse) {
		assertDistribuerForsendelse(distribuerForsendelse);

		//assert DistribuerForsendelse
		final Distribusjonbestilling distBestilling = distribuerForsendelse.getDistribusjonbestilling();
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
		Person mottaker = (Person) distBestilling.getMottaker();
		assertNotNull(mottaker);
		assertEquals(mottaker.getPersonidentifikator(), PERSON_IDENTIFIKATOR_MOTTAKER);
		assertEquals(mottaker.getNavn(), PERSON_NAVN_MOTTAKER);

		//assert bruker Person
		Person bruker = (Person) distBestilling.getBruker();
		assertNotNull(bruker);
		assertEquals(bruker.getPersonidentifikator(), PERSON_IDENTIFIKATOR_BRUKER);
		assertEquals(bruker.getNavn(), PERSON_NAVN_BRUKER);

		//assert norsk postadresse
		assertNorskPostadresseTo(distBestilling);

		//assert dokumenter
		Assertions.assertThat(distBestilling.getDokumenter())
				.extracting(DokumentInformasjon::getDokumenttypeId,
						DokumentInformasjon::getDokumentObjektReferanse,
						DokumentInformasjon::getTilknyttetSom,
						DokumentInformasjon::getArkivDokumentInfoId,
						DokumentInformasjon::getRekkefolge)
				.hasSize(2)
				.containsExactlyInAnyOrder(tuple(DOKUMENTTYPE_ID_1, null, TILKNYTTET_SOM_HOVEDDOK, ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1),
						tuple(DOKUMENTTYPE_ID_2, null, TILKNYTTET_SOM_VEDLEGG, ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2));
	}

	private void assertDistribuerForsendelse(DistribuerForsendelse distribuerForsendelse) {
		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());
	}

	private void assertNorskPostadresseTo(Distribusjonbestilling distBestilling) {
		assertTrue(distBestilling.getAdresse() instanceof NorskPostadresse);
		final NorskPostadresse postadresse = (NorskPostadresse) distBestilling.getAdresse();
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLand(), LAND);
	}

	private void assertUtenlandskPostadresseTo(Distribusjonbestilling distBestilling) {
		assertTrue(distBestilling.getAdresse() instanceof UtenlandskPostadresse);
		final UtenlandskPostadresse postadresse = (UtenlandskPostadresse) distBestilling.getAdresse();
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getLand(), LAND);
	}

	private HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder createDistribusjonbestillingToBuilder() {

		return HentDokumenterFraJoarkTo.DistribusjonbestillingTo.builder()
				.bestillingsId(BESTILLINGS_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.tema(TEMA)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.arkivInformasjon(HentDokumenterFraJoarkTo.ArkivInformasjonTo.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID)
						.build())
				.mottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER))
				.bruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER))
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.dokumenter(Arrays.asList(
						HentDokumenterFraJoarkTo.DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_1)
								.variantFormat(VARIANTFORMAT_1)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
								.rekkefolge(REKKEFOLGE_1)
								.build(),
						HentDokumenterFraJoarkTo.DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_2)
								.variantFormat(VARIANTFORMAT_2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
								.rekkefolge(REKKEFOLGE_2)
								.build())
				);
	}

	private HentDokumenterFraJoarkTo.NorskPostadresseTo createNorskPostadresse() {
		return new HentDokumenterFraJoarkTo.NorskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND, POSTNUMMER, POSTSTED);
	}

	private HentDokumenterFraJoarkTo.UtenlandskPostadresseTo createUtenlandskPostadresse() {
		return new HentDokumenterFraJoarkTo.UtenlandskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND);
	}

	private HentDokumenterFraJoarkTo.AktoerTo createAktoerPerson(String navn, String identifikator) {
		return new HentDokumenterFraJoarkTo.AktoerTo(identifikator, navn, false, AktoerTypeCode.PERSON);
	}

	private HentDokumenterFraJoarkTo.AktoerTo createMottakerAktoerId(String navn, String identifikator) {
		return new HentDokumenterFraJoarkTo.AktoerTo(identifikator, navn, true, AktoerTypeCode.PERSON);
	}

	private HentDokumenterFraJoarkTo.AktoerTo createAktoerOrganisasjon(String navn, String identifikasjon) {
		return new HentDokumenterFraJoarkTo.AktoerTo(identifikasjon, navn, false, AktoerTypeCode.ORGANISASJON);
	}

	private HentDokumenterFraJoarkTo.AktoerTo createAktoerSamhandlerHpr(String navn, String identifikasjon) {
		return new HentDokumenterFraJoarkTo.AktoerTo(identifikasjon, navn, false, AktoerTypeCode.SAMHANDLER_HPR);
	}

	private HentDokumenterFraJoarkTo.AktoerTo createAktoerSamhandlerUtlOrg(String navn, String identifikasjon) {
		return new HentDokumenterFraJoarkTo.AktoerTo(identifikasjon, navn, false, AktoerTypeCode.SAMHANDLER_UTL_ORG);
	}
}