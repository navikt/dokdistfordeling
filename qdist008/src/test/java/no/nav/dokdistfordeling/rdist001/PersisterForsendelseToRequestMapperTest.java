package no.nav.dokdistfordeling.rdist001;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.nav.dokdistfordeling.consumer.aktoerv2.HentIdentForAktoerIdResponseTo;
import no.nav.dokdistfordeling.consumer.tkat020.DokumenttypeInfoTo;
import no.nav.dokdistfordeling.kodeverk.ArkivSystemCode;
import no.nav.dokdistfordeling.kodeverk.DistribusjonsKanalCode;
import no.nav.dokdistfordeling.kodeverk.AktoerTypeCode;
import no.nav.dokdistfordeling.kodeverk.TemaCode;
import no.nav.dokdistfordeling.kodeverk.TilknyttetSomCode;
import no.nav.dokdistfordeling.qdist008.DistribuerForsendelseTo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class PersisterForsendelseToRequestMapperTest {

	private static final String FORSENDELSE_TITTEL = "forsendelseTittel";
	private static final String BESTILLINGS_ID = "bestillingsId";
	private static final String BATCH_ID = "batchId";
	private static final String BESTILLENDE_FAGSYSTEM = "bestillendeFagsystem";
	private static final String ARKIV_ID = "arkivId";
	private static final String PERSON_IDENTIFIKATOR = "personId";
	private static final String AKTOER_IDENTIFIKATOR = "aktoerId";
	private static final String ORGNUMMER = "orgnr";
	private static final String SAMHANDLER_IDENTIFIKATOR = "samhandlerId";
	private static final String ADRESSELINJE_1 = "adresselinje1";
	private static final String ADRESSELINJE_2 = "adresselinje2";
	private static final String ADRESSELINJE_3 = "adresselinje3";
	private static final String POSTNUMMER = "postnummer";
	private static final String POSTSTED = "poststed";
	private static final String LAND_NORGE = "land_norge";
	private static final String UTLAND = "utland";
	private static final String DOKUMENT_PROD_APP = "dokumentProdApp";
	private static final String DOKUMENTTYPE_ID_1 = "dokumenttypeId1";
	private static final String DOKUMENTTYPE_ID_2 = "dokumenttypeId2";
	private static final String OBJEKT_REFERANSE_1 = "objektReferanse1";
	private static final String OBJEKT_REFERANSE_2 = "objektReferanse2";
	private static final String ARKIV_DOKUMENTINFO_ID_1 = "arkivDokumentinfoId1";
	private static final String ARKIV_DOKUMENTINFO_ID_2 = "arkivDokumentinfoId2";
	private static final int REKKEFOLGE_1 = 1;
	private static final int REKKEFOLGE_2 = 2;
	private static final String MOTTAKERNAVN = "mottakernavn";
	private static final AktoerTypeCode PERSON_TYPE_CODE = AktoerTypeCode.PERSON;
	private static final AktoerTypeCode SAMHANDLER_TYPE_CODE = AktoerTypeCode.SAMHANDLER_HPR;
	private static final AktoerTypeCode ORGANISASJON_TYPE_CODE = AktoerTypeCode.ORGANISASJON;
	private static final TemaCode TEMA = TemaCode.FS22;
	private static final TilknyttetSomCode TILKNYTTET_SOM_CODE_1 = TilknyttetSomCode.HOVEDDOKUMENT;
	private static final TilknyttetSomCode TILKNYTTET_SOM_CODE_2 = TilknyttetSomCode.VEDLEGG;
	private static final ArkivSystemCode ARKIV_SYSTEM_CODE = ArkivSystemCode.JOARK;
	private static final String DOKUMENTTITTEL = "dokumentTittel";

	private static final DistribusjonsKanalCode DISTRIBUSJONS_KANAL_CODE = DistribusjonsKanalCode.PRINT;

	private PersisterForsendelseToRequestMapper persisterForsendelseToRequestMapper = new PersisterForsendelseToRequestMapper();

	// Happy path: Person with norsk postaddresse
	@Test
	public void shouldMapHappyPath() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder().build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().build());

		assertCommon(persisterForsendelseRequestTo);
		assertMottakerIsPerson(persisterForsendelseRequestTo.getMottaker());
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertNorskPostaddresseTo(persisterForsendelseRequestTo.getPostadresse());
	}

	@Test
	public void shouldMapWithAktoerId() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerToWithAktoerId())
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(AKTOER_IDENTIFIKATOR).build());

		assertMottakerIsAktoerId(persisterForsendelseRequestTo.getMottaker());
	}


	@Test
	public void shouldMapWithOrganisasjonsNr() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerToWithOrganisasjonsNr())
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());

		assertMottakerIsOrganisasjonNr(persisterForsendelseRequestTo.getMottaker());
	}

	@Test
	public void shouldMapWithSamhandler() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.mottaker(createMottakerToWithSamhandler())
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());

		assertMottakerWithSamhandler(persisterForsendelseRequestTo.getMottaker());
	}

	@Test
	public void shouldMapWithUtenlandsPostaddresse() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.adresse(createUtenlandsPostadresseTo())
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());

		assertResponseIsPostaddresseUtenlands(persisterForsendelseRequestTo.getPostadresse());
	}

	@Test
	public void shouldMapWithNullAddresse() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.adresse(null)
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());

		assertNull(persisterForsendelseRequestTo.getPostadresse());
	}

	@Test
	public void shouldMapWithTomTittel() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.forsendelseTittel(null)
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());
		assertResponseWithTomTittel(persisterForsendelseRequestTo);
	}

	@Test
	public void shouldMapWithNullArkivinformasjon() {
		PersisterForsendelseRequestTo persisterForsendelseRequestTo = performMapping(createDistribusjonbestillingToBuilder()
						.arkivInformasjon(null)
						.build(),
				DokumenttypeInfoTo.builder().dokumentTittel(DOKUMENTTITTEL).build(),
				HentIdentForAktoerIdResponseTo.builder().foedselsnr(PERSON_IDENTIFIKATOR).build());

		assertNull(persisterForsendelseRequestTo.getArkivInformasjon());
	}


	private void assertCommon(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertEquals(BATCH_ID, persisterForsendelseRequestTo.getBatchId());
		assertEquals(BESTILLENDE_FAGSYSTEM, persisterForsendelseRequestTo.getBestillendeFagsystem());
		assertEquals(BESTILLINGS_ID, persisterForsendelseRequestTo.getBestillingsId());
		assertEquals(DOKUMENT_PROD_APP, persisterForsendelseRequestTo.getDokumentProdApp());
		assertEquals(TEMA, persisterForsendelseRequestTo.getTema());

		assertEquals(ARKIV_ID, persisterForsendelseRequestTo.getArkivInformasjon().getArkivId());
		assertEquals(ARKIV_SYSTEM_CODE, persisterForsendelseRequestTo.getArkivInformasjon().getArkivSystem());
	}

	private void assertMottakerIsPerson(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(PERSON_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(PERSON_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	private void assertMottakerIsAktoerId(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(PERSON_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(AKTOER_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	private void assertMottakerIsOrganisasjonNr(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(ORGANISASJON_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(ORGNUMMER, mottaker.getMottakerId());
	}

	private void assertMottakerWithSamhandler(PersisterForsendelseRequestTo.MottakerTo mottaker) {
		assertEquals(SAMHANDLER_TYPE_CODE, mottaker.getMottakerType());
		assertEquals(MOTTAKERNAVN, mottaker.getMottakerNavn());
		assertEquals(SAMHANDLER_IDENTIFIKATOR, mottaker.getMottakerId());
	}

	private void assertNorskPostaddresseTo(PersisterForsendelseRequestTo.PostadresseTo postadresseTo) {
		assertEquals(ADRESSELINJE_1, postadresseTo.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresseTo.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresseTo.getAdresselinje3());
		assertEquals(POSTNUMMER, postadresseTo.getPostnummer());
		assertEquals(POSTSTED, postadresseTo.getPoststed());
		assertEquals(LAND_NORGE, postadresseTo.getLandkode());
	}

	private void assertResponseIsPostaddresseUtenlands(PersisterForsendelseRequestTo.PostadresseTo postadresseTo) {
		assertEquals(ADRESSELINJE_1, postadresseTo.getAdresselinje1());
		assertEquals(ADRESSELINJE_2, postadresseTo.getAdresselinje2());
		assertEquals(ADRESSELINJE_3, postadresseTo.getAdresselinje3());
		assertEquals(UTLAND, postadresseTo.getLandkode());
	}

	private void assertResponseWithTomTittel(PersisterForsendelseRequestTo persisterForsendelseRequestTo) {
		assertDokumentInformasjon(persisterForsendelseRequestTo.getDokumenter());
		assertEquals(DOKUMENTTITTEL, persisterForsendelseRequestTo.getForsendelseTittel());
	}

	private void assertDokumentInformasjon(List<PersisterForsendelseRequestTo.DokumentTo> dokumenter) {

		assertEquals(DOKUMENTTYPE_ID_1, dokumenter.get(0).getDokumenttypeId());
		assertEquals(ARKIV_DOKUMENTINFO_ID_1, dokumenter.get(0).getArkivDokumentInfoId());
		assertEquals(TILKNYTTET_SOM_CODE_1, dokumenter.get(0).getTilknyttetSom());
		assertEquals(ARKIV_DOKUMENTINFO_ID_1, dokumenter.get(0).getArkivDokumentInfoId());
		assertEquals(REKKEFOLGE_1, dokumenter.get(0).getRekkefolge());

		assertEquals(DOKUMENTTYPE_ID_2, dokumenter.get(1).getDokumenttypeId());
		assertEquals(ARKIV_DOKUMENTINFO_ID_2, dokumenter.get(1).getArkivDokumentInfoId());
		assertEquals(TILKNYTTET_SOM_CODE_2, dokumenter.get(1).getTilknyttetSom());
		assertEquals(ARKIV_DOKUMENTINFO_ID_2, dokumenter.get(1).getArkivDokumentInfoId());
		assertEquals(REKKEFOLGE_2, dokumenter.get(1).getRekkefolge());
	}

	private List<DistribuerForsendelseTo.DokumentInformasjonTo> createDokumentInformasjonToListe() {
		return Arrays.asList(createFirstDistribuerForsendelseToBuilder().build(),
				createSecondDistribuerForsendelseToBuilder().build());
	}

	private PersisterForsendelseRequestTo performMapping(DistribuerForsendelseTo.DistribusjonbestillingTo distribusjonbestillingTo,
														 DokumenttypeInfoTo dokumenttypeInfoTo,
														 HentIdentForAktoerIdResponseTo hentIdentForAktoerIdResponseTo) {
		return persisterForsendelseToRequestMapper.map(
				distribusjonbestillingTo,
				dokumenttypeInfoTo,
				hentIdentForAktoerIdResponseTo,
				DISTRIBUSJONS_KANAL_CODE);
	}

	private DistribuerForsendelseTo.DistribusjonbestillingTo.DistribusjonbestillingToBuilder createDistribusjonbestillingToBuilder() {
		return DistribuerForsendelseTo.DistribusjonbestillingTo.builder()
				.adresse(createNorskPostadresseTo())
				.arkivInformasjon(createArkivInformasjonTo())
				.batchId(BATCH_ID)
				.bestillendeFagsystem(BESTILLENDE_FAGSYSTEM)
				.bestillingsId(BESTILLINGS_ID)
				.dokumenter(createDokumentInformasjonToListe())
				.dokumentProdApp(DOKUMENT_PROD_APP)
				.forsendelseTittel(FORSENDELSE_TITTEL)
				.mottaker(createMottakerToWithPerson())
				.tema(TemaCode.FS22);
	}

	private DistribuerForsendelseTo.DokumentInformasjonTo.DokumentInformasjonToBuilder createFirstDistribuerForsendelseToBuilder() {
		return DistribuerForsendelseTo.DokumentInformasjonTo.builder()
				.dokumenttypeId(DOKUMENTTYPE_ID_1)
				.dokumentObjektReferanse(OBJEKT_REFERANSE_1)
				.tilknyttetSom(TILKNYTTET_SOM_CODE_1)
				.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_1)
				.rekkefolge(REKKEFOLGE_1);


	}

	private DistribuerForsendelseTo.DokumentInformasjonTo.DokumentInformasjonToBuilder createSecondDistribuerForsendelseToBuilder() {
		return DistribuerForsendelseTo.DokumentInformasjonTo.builder()
				.dokumenttypeId(DOKUMENTTYPE_ID_2)
				.dokumentObjektReferanse(OBJEKT_REFERANSE_2)
				.tilknyttetSom(TILKNYTTET_SOM_CODE_2)
				.arkivDokumentInfoId(ARKIV_DOKUMENTINFO_ID_2)
				.rekkefolge(REKKEFOLGE_2);
	}

	private DistribuerForsendelseTo.AktoerTo createMottakerToWithPerson() {
		return DistribuerForsendelseTo.AktoerTo.builder()
				.identifikator(PERSON_IDENTIFIKATOR)
				.identifikatorAktoerId(false)
				.aktoerType(PERSON_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.AktoerTo createMottakerToWithAktoerId() {
		return DistribuerForsendelseTo.AktoerTo.builder()
				.identifikator(AKTOER_IDENTIFIKATOR)
				.identifikatorAktoerId(true)
				.aktoerType(PERSON_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.AktoerTo createMottakerToWithOrganisasjonsNr() {
		return DistribuerForsendelseTo.AktoerTo.builder()
				.identifikator(ORGNUMMER)
				.identifikatorAktoerId(false)
				.aktoerType(ORGANISASJON_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.AktoerTo createMottakerToWithSamhandler() {
		return DistribuerForsendelseTo.AktoerTo.builder()
				.identifikator(SAMHANDLER_IDENTIFIKATOR)
				.identifikatorAktoerId(false)
				.aktoerType(SAMHANDLER_TYPE_CODE)
				.navn(MOTTAKERNAVN)
				.build();
	}

	private DistribuerForsendelseTo.ArkivInformasjonTo createArkivInformasjonTo() {
		return DistribuerForsendelseTo.ArkivInformasjonTo.builder()
				.arkivSystem(ArkivSystemCode.JOARK)
				.arkivId(ARKIV_ID)
				.build();
	}

	private DistribuerForsendelseTo.NorskPostadresseTo createNorskPostadresseTo() {
		return DistribuerForsendelseTo.NorskPostadresseTo.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.postnummer(POSTNUMMER)
				.poststed(POSTSTED)
				.land(LAND_NORGE)
				.build();
	}

	private DistribuerForsendelseTo.UtenlandskPostadresseTo createUtenlandsPostadresseTo() {
		return DistribuerForsendelseTo.UtenlandskPostadresseTo.builder()
				.adresselinje1(ADRESSELINJE_1)
				.adresselinje2(ADRESSELINJE_2)
				.adresselinje3(ADRESSELINJE_3)
				.land(UTLAND)
				.build();
	}
}