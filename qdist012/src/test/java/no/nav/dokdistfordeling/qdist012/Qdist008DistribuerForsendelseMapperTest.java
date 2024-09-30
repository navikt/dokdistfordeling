package no.nav.dokdistfordeling.qdist012;

import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.AktoerTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.ArkivInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.DokumentInformasjonTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.NorskPostadresseTo;
import no.nav.dokdistfordeling.qdist012.HentDokumenterFraJoarkTo.UtenlandskPostadresseTo;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.ORGANISASJON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.PERSON;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_HPR;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UKJENT;
import static no.nav.dokdistfordeling.kodeverk.AktoerTypeCode.SAMHANDLER_UTL_ORG;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstidspunktCode.KJERNETID;
import static no.nav.dokdistfordeling.kodeverk.DistribusjonstypeCode.VEDTAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.distribusjonstype(VEDTAK)
						.distribusjonstidspunkt(KJERNETID)
						.distribusjonKanal(PRINT.name())
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertResponse(distribuerForsendelse);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " "})
	@NullSource
	public void shouldMapToNullWhenBatchIdIsBlankOrNull(String batchId) {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.batchId(batchId)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getBatchId());
	}

	@Test
	public void shouldMapOkWhenForsendelseTittelIsNull() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.forsendelseTittel(null)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getForsendelseTittel());
	}

	@Test
	public void shouldMapOkWhenArkivinformasjonIsNull() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.arkivInformasjon(null)
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());
		assertNull(distribuerForsendelse.getDistribusjonbestilling().getArkivInformasjon());
	}

	@Test
	public void shouldMapAktoerId() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerAktoerId(MOTTAKER_ID_NAVN, MOTTAKER_ID))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());

		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof AktoerId);
		final AktoerId mottaker = (AktoerId) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(mottaker.getAktoerId(), MOTTAKER_ID);
		assertEquals(mottaker.getNavn(), MOTTAKER_ID_NAVN);
	}

	@Test
	public void shouldMapOrganisasjon() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerOrganisasjon(ORGANISASJON_NAVN, ORGNUMMER))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());

		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof Organisasjon);
		final Organisasjon organisasjon = (Organisasjon) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(organisasjon.getOrgnummer(), ORGNUMMER);
		assertEquals(organisasjon.getNavn(), ORGANISASJON_NAVN);
	}

	@ParameterizedTest
	@MethodSource
	public void shouldMapSamhandler(AktoerTypeCode aktoerTypeCode, String samhandlerkategori) {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.mottaker(createAktoerSamhandler(SAMHANDLER_NAVN, SAMHANDLER_IDENTIFIKATOR, aktoerTypeCode))
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());

		assertNotNull(distribuerForsendelse.getDistribusjonbestilling().getMottaker());
		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getMottaker() instanceof Samhandler);
		final Samhandler samhandler = (Samhandler) distribuerForsendelse.getDistribusjonbestilling().getMottaker();
		assertEquals(samhandler.getSamhandleridentifikator(), SAMHANDLER_IDENTIFIKATOR);
		assertEquals(samhandler.getNavn(), SAMHANDLER_NAVN);
		assertEquals(samhandler.getSamhandlerkategori(), samhandlerkategori);
	}

	private static Stream<Arguments> shouldMapSamhandler() {
		return Stream.of(
				Arguments.of(SAMHANDLER_HPR, "HPR"),
				Arguments.of(SAMHANDLER_UKJENT, "UKJENT"),
				Arguments.of(SAMHANDLER_UTL_ORG, "UTL_ORG")
		);
	}

	@Test
	public void shouldMapUtenlandskAdresse() {
		var hentDokumenterFraJoarkTo = HentDokumenterFraJoarkTo.builder()
				.distribusjonbestilling(createDistribusjonbestillingToBuilder()
						.distribusjonKanal(PRINT.name())
						.adresse(createUtenlandskPostadresse())
						.build())
				.build();

		DistribuerForsendelse distribuerForsendelse = qdist008DistribuerForsendelseMapper.map(hentDokumenterFraJoarkTo);

		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());

		assertTrue(distribuerForsendelse.getDistribusjonbestilling().getAdresse() instanceof UtenlandskPostadresse);
		final UtenlandskPostadresse postadresse = (UtenlandskPostadresse) distribuerForsendelse.getDistribusjonbestilling().getAdresse();
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getLand(), LAND);
	}

	private void assertResponse(DistribuerForsendelse distribuerForsendelse) {
		assertNotNull(distribuerForsendelse);
		assertNotNull(distribuerForsendelse.getDistribusjonbestilling());

		//assert DistribuerForsendelse
		final Distribusjonbestilling distBestilling = distribuerForsendelse.getDistribusjonbestilling();
		assertEquals(distBestilling.getBestillingsId(), BESTILLINGS_ID);
		assertEquals(distBestilling.getBatchId(), BATCH_ID);
		assertEquals(distBestilling.getBestillendeFagsystem(), BESTILLENDE_FAGSYSTEM);
		assertEquals(distBestilling.getTema(), TEMA);
		assertEquals(distBestilling.getForsendelseTittel(), FORSENDELSE_TITTEL);
		assertEquals(distBestilling.getDokumentProdApp(), DOKUMENT_PROD_APP);
		assertEquals(distBestilling.getDistribusjonstype(), VEDTAK.name());
		assertEquals(distBestilling.getDistribusjonstidspunkt(), KJERNETID.name());

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
		assertTrue(distBestilling.getAdresse() instanceof NorskPostadresse);
		final NorskPostadresse postadresse = (NorskPostadresse) distBestilling.getAdresse();
		assertEquals(postadresse.getAdresselinje1(), ADRESSELINJE_1);
		assertEquals(postadresse.getAdresselinje2(), ADRESSELINJE_2);
		assertEquals(postadresse.getAdresselinje3(), ADRESSELINJE_3);
		assertEquals(postadresse.getPostnummer(), POSTNUMMER);
		assertEquals(postadresse.getPoststed(), POSTSTED);
		assertEquals(postadresse.getLand(), LAND);

		//assert dokumenter
		assertThat(distBestilling.getDokumenter())
				.extracting(DokumentInformasjon::getDokumenttypeId,
						DokumentInformasjon::getDokumentObjektReferanse,
						DokumentInformasjon::getTilknyttetSom,
						DokumentInformasjon::getArkivDokumentInfoId,
						DokumentInformasjon::getRekkefolge)
				.hasSize(2)
				.containsExactlyInAnyOrder(
						tuple(DOKUMENTTYPE_ID_1, null, TILKNYTTET_SOM_HOVEDDOK, ARKIV_DOKUMENTINFO_ID_1, REKKEFOLGE_1),
						tuple(DOKUMENTTYPE_ID_2, null, TILKNYTTET_SOM_VEDLEGG, ARKIV_DOKUMENTINFO_ID_2, REKKEFOLGE_2)
				);
	}

	private DistribusjonbestillingToBuilder createDistribusjonbestillingToBuilder() {

		return DistribusjonbestillingTo.builder()
				.bestillingsId(BESTILLINGS_ID)
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.tema(TEMA)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.arkivInformasjon(ArkivInformasjonTo.builder()
						.arkivSystem(ARKIV_SYSTEM)
						.arkivId(ARKIV_ID)
						.build())
				.mottaker(createAktoerPerson(PERSON_NAVN_MOTTAKER, PERSON_IDENTIFIKATOR_MOTTAKER))
				.bruker(createAktoerPerson(PERSON_NAVN_BRUKER, PERSON_IDENTIFIKATOR_BRUKER))
				.adresse(createNorskPostadresse())
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.dokumenter(Arrays.asList(
						DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_1)
								.variantFormat(VARIANTFORMAT_1)
								.tilknyttetSom(TILKNYTTET_SOM_HOVEDDOK)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
								.rekkefolge(REKKEFOLGE_1)
								.build(),
						DokumentInformasjonTo.builder()
								.dokumenttypeId(DOKUMENTTYPE_ID_2)
								.variantFormat(VARIANTFORMAT_2)
								.tilknyttetSom(TILKNYTTET_SOM_VEDLEGG)
								.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
								.rekkefolge(REKKEFOLGE_2)
								.build())
				);
	}

	private NorskPostadresseTo createNorskPostadresse() {
		return new NorskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND, POSTNUMMER, POSTSTED);
	}

	private UtenlandskPostadresseTo createUtenlandskPostadresse() {
		return new UtenlandskPostadresseTo(ADRESSELINJE_1, ADRESSELINJE_2, ADRESSELINJE_3, LAND);
	}

	private AktoerTo createAktoerPerson(String navn, String identifikator) {
		return new AktoerTo(identifikator, navn, false, PERSON);
	}

	private AktoerTo createMottakerAktoerId(String navn, String identifikator) {
		return new AktoerTo(identifikator, navn, true, PERSON);
	}

	private AktoerTo createAktoerOrganisasjon(String navn, String identifikasjon) {
		return new AktoerTo(identifikasjon, navn, false, ORGANISASJON);
	}

	private AktoerTo createAktoerSamhandler(String navn, String identifikasjon, AktoerTypeCode aktoerTypeCode) {
		return new AktoerTo(identifikasjon, navn, false, aktoerTypeCode);
	}

}